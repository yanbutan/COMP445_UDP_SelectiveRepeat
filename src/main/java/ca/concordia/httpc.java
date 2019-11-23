/*
 * Yan Nyein Aung
 * 40151994
 * Lab 1
 */
package ca.concordia;
import java.util.Scanner;
import ca.concordia.ClientClass.Client;
import ca.concordia.ClientClass.UDPClient;


public class httpc{
	public static void main(String[]args) throws Exception{
        // UDPClient udpClient  = new UDPClient();
        // Client client = new Client(udpClient);
		Scanner sc = new Scanner(System.in);
		System.out.print("Enter curl commands below.Type \"done\" to exit.");
		sc.nextLine();
		int localAddrPort = 41830;
        

		String input = "";
		
		while(true){
			UDPClient udpClient  = new UDPClient(localAddrPort);
        	Client client = new Client(udpClient);
			System.out.print(">> ");
			input = sc.nextLine();
			if(input.equals("done")){
				break;
			}
			System.out.println();
			
			try{
				// GET
				if(input.contains("get")){
					String url = input.substring(input.indexOf("http://"), input.length()-1);
					String data= input.substring(input.indexOf("get")+4, input.indexOf("http://")-1);
					System.out.println(client.GET(url, data));
				}
				
				// POST
				else if(input.contains("post")){
					String url = input.substring(input.indexOf("http://"), input.length());
					String data= input.substring(input.indexOf("post")+5, input.indexOf("http://")-1);
					
					System.out.println(client.POST(url, data));
				}

				// Incomplete Portion tied with error 400
				else{
					String url = input.substring(input.indexOf("http://"), input.length());
					String data= input.substring(input.indexOf("http://")-1);
					
					System.out.println(client.INVALID(url, data));
				}
			}catch(StringIndexOutOfBoundsException e){
					System.out.println(client.INVALID("", ""));
			}
			
			localAddrPort++;
			System.out.println("New Local Port Number is " + localAddrPort);
			System.out.println("================================================================================");
			System.out.println("");
		}
	}
}