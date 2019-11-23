package ca.concordia.ServerClass;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

import ca.concordia.Packet;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;

public class UDPServer {
    static String uAgent;
	static String fileName;
	static String wdName;
	static int contLength;
    static String postData;
    static boolean isVerbose;
    static String filePath;
    static int port;
    static BufferedWriter bw;

    private DatagramChannel channel;
    private SocketAddress router;
    
    private static final Logger logger = LoggerFactory.getLogger(UDPServer.class);

    private void listenAndServe(int port) throws IOException {
        
        try (DatagramChannel datagramChannel = DatagramChannel.open()) {
            this.channel = datagramChannel;
            channel.bind(new InetSocketAddress(port));
            logger.info("EchoServer is listening at {}", channel.getLocalAddress());
            ByteBuffer buf = ByteBuffer
                    .allocate(Packet.MAX_LEN)
                    .order(ByteOrder.BIG_ENDIAN);

            for (; ; ) {
                buf.clear();
                this.router = channel.receive(buf);

                // Parse a packet from the received raw data.
                buf.flip();
                Packet packet = Packet.fromBuffer(buf);
                buf.flip();

                if(packet.getType() == 0){
                    System.out.println("Handshake is done, server is connected");
                    packet.toBuilder().setType(1);
                    
                    String payload = new String(packet.getPayload(), UTF_8);
                    logger.info("Packet: {}", packet);
                    logger.info("Payload: {}", payload);
                    logger.info("Router: {}", router);

                    if(isVerbose){
                        System.out.println(payload);
                    }

                    long seqNum = packet.getSequenceNumber()+1;
                    String handledRequest = handleRequests(payload);

                     // Send the response to the router not the client.
                    // The peer address of the packet is the address of the client already.
                    // We can use toBuilder to copy properties of the current packet.
                    // This demonstrate how to create a new packet from an existing packet.
                    Packet resp = packet.toBuilder().setSequenceNumber
                                  (seqNum).setType(1).setPayload(handledRequest.getBytes()).create();
                    this.channel.send(resp.toBuffer(), router);         
                }
                else{
                    handShake(packet);
                }

            }
        }
    }

    private String handleRequests(String inputString) throws IOException {
        uAgent = "";
        String response = "";
        System.out.println(inputString);
        String header = inputString.split("\r\n\r\n")[0];

        if(inputString.contains("POST")){
            postData = inputString.split("\r\n\r\n")[1];
        }

        String[] headerArray = header.split("\r\n");
        String firstLine = headerArray[0];
        
        for(int i=0;i<headerArray.length;i++){
            if (headerArray[i].contains("User-Agent:")){
                uAgent = headerArray[i].substring(11); 
            }
            if (headerArray[i].contains("Content-Length:")){
                contLength = Integer.parseInt(headerArray[i].substring(15).trim());
            }
        }
            
        //Incomplete
        if(inputString.startsWith("invalid") || !inputString.contains("HTTP/1.0")){
            // bad request	
            System.out.println("invalid request here here ");
            invalidRequest();
        }
        else{
            if(firstLine.startsWith("GET")){
                fileName = firstLine.substring(3, firstLine.length()-8).trim();
            }
            else if(firstLine.startsWith("POST")){
                fileName = firstLine.substring(4, firstLine.length()-8).trim();
            }
            String fullPath = filePath + fileName;
            File file = new File(fullPath);
            
            //There is quite a few scenarios i can think of for this 
            //But i dont know of a general solution to cover all cases
            //And i dont have enough time to implement every single one
            //Is there a more efficient way to do this?
            if(fullPath.contains(wdName + "/..")){
                System.out.println("Request is not authorized");    
                System.out.println(file.getAbsolutePath());
                response = forbiddenAccess(fileName);
            }
            else if(!file.exists() && !inputString.contains("POST")){
                System.out.println("File with path " + fullPath + " don't exist");
                response = fileNotFound(fileName);
            }
            else{
                String s;
                if(inputString.startsWith("GET")){
                    System.out.println("GET Request received with file name " + fileName + " and full path " + fullPath);
                    
                    if(fileName.equals("/")){
                        String[] pathnames = file.list();
                        response = sendFile(pathnames);
                    }
                    else{
                        FileReader fr = new FileReader(file);
                        BufferedReader buffRead = new BufferedReader(fr);

                        response = "HTTP/1.0 200 OK\r\n" + 
                                            "User-Agent: " + uAgent + "\r\n" +
                                            "Content-Length: " + Files.readAllBytes(Paths.get(file.getAbsolutePath())).length + "\r\n" +
                                            "Content-Type: " + getContentType(fullPath) +"\r\n"+"\r\n"; 
                                            

                        while((s = buffRead.readLine()) != null) {
                            response += s + "\r\n";
                        } 
                        fr.close();
                    }
                }
                else{
                    System.out.println("POST Request");
                    if(!file.exists()){
                        File newFile = new File(fullPath);
                        newFile.getParentFile().mkdirs();
                        newFile.createNewFile();
                        file = newFile;
                        response = "HTTP/1.0 201 OK\r\n" + "File created\r\n";
                    }
                    else{
                        response = "HTTP/1.0 200 OK\r\n" + "File content overwrote\r\n";
                    }
                    FileReader fr = new FileReader(file);
                    BufferedReader buffRead = new BufferedReader(fr);
                    bw = new BufferedWriter(new FileWriter(file));
                    bw.write(postData);
                    bw.close();

                    response += "User-Agent: " + uAgent + "\r\n" +
                    "Content-Length: " + Files.readAllBytes(Paths.get(file.getAbsolutePath())).length + "\r\n" +
                    "Content-Type: " + getContentType(fullPath) +"\r\n"+"\r\n";

                    while((s = buffRead.readLine()) != null) {
                        response += s + "\r\n";
                    } 
                    fr.close();

                    System.out.println("Done check the file");			
                }
            }
        }
        return response;
    }

    //Returns the file based on the path 
	//For now this is done for GET all files
	private String sendFile(String[] files)throws IOException {
		String response = "";
		for(int i = 0;i < files.length;i++){
			if(!files[i].contains(".class") && !files[i].contains(".java")){
				System.out.println(files[i]);
				response += files[i] + "\r\n";
			} 
		}
		return response;
	}
	
	private String invalidRequest() throws IOException {
		String errMsg = "The web server only understands GET or POST requests\r\n";
		errMsg += "HTTP/1.0 400 Bad Request\r\n";
		errMsg += "User-Agent: " + uAgent + "\r\n";
		errMsg += "Content-length: " + errMsg.length() + "\r\n\r\n";
		return errMsg;
    }

    private String fileNotFound(String fileName) throws IOException {
		String errMsg = "File name : " + fileName + " does not exist.\r\n";
		errMsg += "HTTP/1.0 404 Not Found\r\n";
		errMsg += "User-Agent: " + uAgent + "\r\n";
		errMsg += "Content-length: " + errMsg.length() + "\r\n\r\n";
		return errMsg;	
    }

    private String forbiddenAccess(String fileName) throws IOException {
		String errMsg = "You are not authorized to access " + fileName + ".\r\n";
		errMsg += "HTTP/1.0 403 Forbidden\r\n";
		errMsg += "User-Agent: " + uAgent + "\r\n";
		errMsg += "Content-length: " + errMsg.length() + "\r\n\r\n";
		return errMsg;
	}
	
	// private String sendMessage(String msg) throws IOException {
	// 	// byte [] respBytes = msg.getBytes();
	// 	// OutputStream os = connection.getOutputStream();
	// 	// BufferedOutputStream bos = new BufferedOutputStream(os);
	// 	// bos.write(respBytes, 0 , respBytes.length);
    //     // bos.flush();
    //     return msg;
	// }

	private static String getContentType(String path){
		if (path.endsWith(".html") || path.endsWith(".htm"))
			return "text/html";
		else if (path.endsWith(".txt") || path.endsWith(".java"))
			return "text/plain";
		else if (path.endsWith(".gif"))
			return "image/gif";
		else if (path.endsWith(".class"))
			return "application/octet-stream";
		else if (path.endsWith(".jpg") || path.endsWith(".jpeg"))
			return "image/jpeg";
		else
			return "text/plain";
    }

    private void handShake(Packet packet) throws IOException {
        System.out.println("Handshake #1 SYN packet recived");
        System.out.println("Message is this : " + new String(packet.getPayload(), StandardCharsets.UTF_8));
        String temp = "Hi wassuppp";
        Packet response = packet.toBuilder().setSequenceNumber(packet.getSequenceNumber() + 1).setType(3).setPayload(temp.getBytes()).create();
        channel.send(response.toBuffer(), router);
        System.out.println("Handshake #2 SYN packet has sent out");
    }

    public static void main(String[] args) throws IOException {
        OptionParser parser = new OptionParser();
        parser.acceptsAll(asList("port", "p"), "Listening port")
                .withOptionalArg()
                .defaultsTo("8007");
        System.out.println("Enter server options below. E.g httpfs [-v] [-p PORT] [-d PATH-TO-DIR]");
        Scanner sc = new Scanner(System.in);
        String para = sc.nextLine();

        isVerbose = false;
        filePath = System.getProperty("user.dir");

		if(para.contains("-v")){
			isVerbose = true;
		}

		if(para.contains("-p")){
			String s = para.substring(para.indexOf("-p") + 3);
			s = s.substring(0, s.indexOf(" "));
			port = Integer.parseInt(s);
		}

		if(para.contains("-d")){
			String s = para.substring(para.indexOf("-d") + 3);
			wdName = s.substring(0, s.indexOf(" "));
			filePath += wdName;
        }
        
        OptionSet opts = parser.parse(args);
        int port = Integer.parseInt((String) opts.valueOf("port"));
        UDPServer server = new UDPServer();
        server.listenAndServe(port);
    }
}