package ca.concordia.ClientClass;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import ca.concordia.Packet;

import static java.nio.channels.SelectionKey.OP_READ;

public class UDPClient {
    private Long seq_num = 1L;
    private Long server_seq_num = 1L;
    private static final Logger logger = LoggerFactory.getLogger(UDPClient.class);
    private SocketAddress localAddr;
    private SocketAddress routerAddr;
    private boolean isHandShaken;

    public UDPClient(int localPort){
        this.localAddr = new InetSocketAddress(localPort);
        this.routerAddr = new InetSocketAddress("localhost",3000);
    }

    public String runClient(InetSocketAddress serverAddr, String request) throws IOException {
        String payload = "";
        boolean correct_seq = false;

        try(DatagramChannel channel = DatagramChannel.open()){
            channel.bind(this.localAddr);
            seq_num = threeWayHandShake(channel, serverAddr);
            if(this.isHandShaken){
                Packet p = null;
                if(request.getBytes().length <= Packet.MAX_LEN){
                    p = new Packet.Builder()
                            .setType(0)
                            .setSequenceNumber(seq_num + 1)
                            .setPortNumber(serverAddr.getPort())
                            .setPeerAddress(serverAddr.getAddress())
                            .setPayload(request.getBytes())
                            .create();
                }else{
                    return "I don't know how to split into 2 packets lol";
                }
                channel.send(p.toBuffer(), routerAddr);
                logger.info("Sending \"{}\" to router at {}", request, routerAddr);

                // Try to receive a packet within timeout.
                timer(channel, p);

                // We just want a single response.
                ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
                routerAddr = channel.receive(buf);
                buf.flip();
                Packet resp = Packet.fromBuffer(buf);
                logger.info("Packet: {}", resp);
                logger.info("Router: {}", routerAddr);
                // if(resp.getType() == 1){
                //     payload = new String(resp.getPayload(), StandardCharsets.UTF_8);
                //     logger.info("Payload: {}",  payload);
                //     return payload;
                // }
                payload = new String(resp.getPayload(), StandardCharsets.UTF_8);
                logger.info("Payload: {}",  payload);
                return payload;
            }
            return null;
        }
    }

    private long threeWayHandShake(DatagramChannel channel,InetSocketAddress serverAddr) throws IOException {

        String testString = "Hi Wassup This is a Hand Shake";
        Packet test = new Packet.Builder()
                .setType(Packet.SYN)
                .setSequenceNumber(seq_num)
                .setPortNumber(serverAddr.getPort())
                .setPeerAddress(serverAddr.getAddress())
                .setPayload(testString.getBytes())
                .create();
        channel.send(test.toBuffer(), routerAddr);
        System.out.println("Sent out number 1 SYN packet");

        timer(channel,test);

        ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
        buf.clear();
        channel.receive(buf);
        buf.flip();
        Packet p = Packet.fromBuffer(buf);

        System.out.println("Server Message"+new String(p.getPayload(), StandardCharsets.UTF_8));
        this.isHandShaken = true;
        System.out.println("Three-way handshake is succesful.You may start sending data.");
        System.out.println("\r\n");
        return p.getSequenceNumber();
    }

    private void timer(DatagramChannel channel,Packet packet) throws IOException {
        // Try to receive a packet within timeout.
        channel.configureBlocking(false);
        Selector selector = Selector.open();
        channel.register(selector, OP_READ);
        selector.select(5000);

        Set<SelectionKey> keys = selector.selectedKeys();
        if (keys.isEmpty()) {
            logger.error("No response after timeout");
            channel.send(packet.toBuffer(), routerAddr);
            timer(channel,packet);
        }
        keys.clear();
        return;
    }    
}

