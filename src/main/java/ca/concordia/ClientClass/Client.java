	/*
 * Yan Nyein Aung
 * 40151994
 * Lab 1
 */

package ca.concordia.ClientClass;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Client {
	private UDPClient udpClient;
	InetSocketAddress inetSocketAddr;

	public Client(UDPClient udpClient) {
		this.udpClient = udpClient;
	}

	// GET method
	public String GET(String url, String str) throws URISyntaxException, IOException {
        String response = "";
        URI uri =  new URI(url);
		String host = uri.getHost();
		String path = uri.getRawPath();
		String query = uri.getRawQuery();
		int port = uri.getPort();
		String uAgent = Client.class.getName();

		if(path.length() == 0 || path == null)
			path = "/";

		if(query != null)
			query = "?" + query;
		else 
			query = "";

		if(port == -1)
			port = 80;

		//Binds serverAddress to specific host and port
		// SocketAddress serverAddress = new InetSocketAddress(host, port);
        if(str.contains("-h ")){
            
            if(str.contains("Host"))
                //Index of ":" after index of "Host" and index of " " after host
                //Substring of this gives the value of key "Host"
                host = str.substring(str.indexOf(":", str.indexOf("Host")) +1, str.indexOf(" ", str.indexOf("Host")));
            if(str.contains("User-Agent"))
                uAgent = str.substring(str.indexOf(":", str.indexOf("User-Agent")) +1, str.indexOf(" ", str.indexOf("User-Agent")));
        }

        String request = "GET " + path + query + " HTTP/1.0\r\n" +
                "Host: " + host + "\r\n" + "Connection: close\r\n" +
                "User-Agent: " + uAgent + "\r\n" + "\r\n";
		inetSocketAddr = new InetSocketAddress(host,port);
		response = udpClient.runClient(inetSocketAddr,request);
		return response;
	}

	// POST method
	public String POST(String url, String str) throws URISyntaxException, IOException {
        String response = "";        
        String body = "";
		URI uri =  new URI(url);
		String host = uri.getHost();
		String path = uri.getRawPath();
		String query = uri.getRawQuery();
		int port = uri.getPort();
		String contType = "";
		int contLength = 0;
		String uAgent = Client.class.getName();

		System.out.println(path);
		if(path.length() == 0 || path == null)
			path = "/";

		if(query == null)
			query = "";
		else 
			query = "?" + query;

		if(port == -1)
			port = 80;

        if(str.contains("-h ")){
            if(str.contains("Host"))
                host = str.substring(str.indexOf(":", str.indexOf("Host")) +1, str.indexOf(" ", str.indexOf("Host")));
            if(str.contains("Content-Type"))
                contType = str.substring(str.indexOf(":", str.indexOf("Content-Type")) +1, str.indexOf(" ", str.indexOf("Content-Type")));
            if(str.contains("Content-Length"))
                contLength = Integer.parseInt(str.substring(str.indexOf(":", str.indexOf("Content-Length")) +1, str.indexOf(" ", str.indexOf("Content-Length"))));
            if(str.contains("User-Agent"))
                uAgent = str.substring(str.indexOf(":", str.indexOf("User-Agent")) +1, str.indexOf(" ", str.indexOf("User-Agent")));
        }

        if(str.contains("-d ")){
            body = str.substring(str.indexOf("-d")+4, str.length()-1);
            contLength = body.length();
            }

        // System.out.println(body);			
        String request = "POST " + path + " HTTP/1.0\r\n" +
                "Host: " + host + "\r\n" + "Content-Length: " + contLength + "\r\n" +
                "Content-Type: " + contType + "\r\n" + "User-Agent: " + uAgent +
                "\r\n" + "\r\n" + body;
		inetSocketAddr = new InetSocketAddress(host,port);
		response = udpClient.runClient(inetSocketAddr,request);
        return response;
	
	}

	/*str: whole command line input*/
    // private String buildResponse(String payload, String str) throws IOException{


    //     FileOperation fileOperation = new FileOperation();
    //     String response = payload;

    //     /*verbose requirement*/
    //     if(str.contains("-v")) {//case that needs verbose
    //         if(needOutputFile(str)){//case need to output body data
    //             fileOperation.writeFile(response,str.substring(str.indexOf("-o") + 3));
    //         }
    //         return response;
    //     }else {//case that does not need verbose
    //         response = response.substring(response.indexOf("{"),response.lastIndexOf("}")+ 1);
    //         if(needOutputFile(str)){//case need to output body data
    //             fileOperation.writeFile(response,str.substring(str.indexOf("-o") + 3));
    //         }
    //     }
    //     return response;
    // }

    // private boolean needOutputFile(String str){
    //     if(str.contains("-o")){
    //         return true;
    //     }
    //     return false;
    // }
// Incomplete Method to handle error 400
	public String INVALID(String url, String str) throws URISyntaxException{

		String response = "";
		FileOutputStream outputstream = null;


		URI uri =  new URI(url);
		String host = uri.getHost();
		String path = uri.getRawPath();
		String query = uri.getRawQuery();
		int port = uri.getPort();
		String uAgent = Client.class.getName();

		if(path.length() == 0 || path == null)
			path = "/";

		if(query != null)
			query = "?" + query;
		else 
			query = "";

		if(port == -1)
			port = 80;


		//Binds serverAddress to specific host and port
		SocketAddress serverAddress = new InetSocketAddress(host, port);

		try(SocketChannel server = SocketChannel.open()) {

			server.connect(serverAddress);

			if(str.contains("-h ")){
				
				if(str.contains("Host"))
					//Index of ":" after index of "Host" and index of " " after host
					//Substring of this gives the value of key "Host"
					host = str.substring(str.indexOf(":", str.indexOf("Host")) +1, str.indexOf(" ", str.indexOf("Host")));
				if(str.contains("User-Agent"))
					uAgent = str.substring(str.indexOf(":", str.indexOf("User-Agent")) +1, str.indexOf(" ", str.indexOf("User-Agent")));
			}
			//Catch different errors and send them to a method
			//Method should have the sending portion to send the request
			//I think the request can just have "invalid" in it lol
			String request = "invalid " + path + query + " HTTP/1.0\r\n" +
					"Host: " + host + "\r\n" + "Connection: close\r\n" +
					"User-Agent: " + uAgent + "\r\n" + "\r\n";


			Charset charset = StandardCharsets.UTF_8;
			ByteBuffer byteBuffer = charset.encode(request);
			server.write(byteBuffer);

			//Buffer in memory and stores the data written by the writablebytechannel
			ByteArrayOutputStream os = new ByteArrayOutputStream(); 
			ByteBuffer data = ByteBuffer.allocateDirect(32 * 1024);

			//A channel used to write byte  
			WritableByteChannel destination = Channels.newChannel(os);  
			
			while(server.read(data) != -1){
				//Flipped to set limit to current position in bytebuffer
				data.flip();
				//Writes sequence of bytes to bytearrayoutputstream from the given bytebuffer 
				destination.write(data);
				//Resets limit to the byte buffer capacity
				data.clear();

				//Returns current conent of output stream as byte array and strigifies them
				response = new String(os.toByteArray(), "UTF-8");

				break;
			}	

		} catch (Exception e) {
			System.out.print("An unexpected error has occured.");
		}

		// if(!str.contains("-v "))
		// 	return response.substring(response.indexOf("{"), response.lastIndexOf("}")+1);
		return response;	
	}
	
}