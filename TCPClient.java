/*
 * Eirini Kyriakou
 */
import java.io.*;
import java.net.*;
import java.util.StringTokenizer;
import java.util.*;

public class TCPClient {

	public static void main(String argv[]) throws Exception {
		String sentence;
		String modifiedSentence;
        String IP;
        int Port;
		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

        System.out.print("Enter IP you want to connect: ");
		IP = inFromUser.readLine();
        System.out.println("Enter port you want to connect:");
		Scanner scanner = new Scanner(System.in);
        Port = scanner.nextInt(); 
		Socket clientSocket = new Socket(IP, Port);

		DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
		BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

		System.out.print("Insert type of Request ('GET' or 'PUT'): ");
		sentence = inFromUser.readLine();
		StringTokenizer tokenizedLine = new StringTokenizer(sentence);
		String token = tokenizedLine.nextToken();
		if (token.equals("GET")) {
			System.out.print("Insert key: ");
			String key = inFromUser.readLine();
			sentence = "Client GET " + key +" HTTP/1.1 to " + IP + " " + Port;
			System.out.println("Request that will be send:\n\t" + sentence);
			outToServer.writeBytes(sentence + "\n");
		}else if(token.equals("PUT")){
			System.out.print("Insert key: ");
			String key = inFromUser.readLine();
			sentence = "Client PUT " + key + " ";
			System.out.print("Insert value: ");
			String value = inFromUser.readLine();
			sentence += value +" HTTP/1.1 to " + IP + " " + Port;
			System.out.println("\nRequest that will be send: \n" + sentence);
			outToServer.writeBytes(sentence + "\n");
		}else{
			System.out.println("Not valid input!!!");
			System.exit(0);
		}

		modifiedSentence = inFromServer.readLine();
		System.out.print("\nFROM SERVER:\n\t");
		while (modifiedSentence != null) {
			System.out.println(modifiedSentence);
			modifiedSentence = inFromServer.readLine();
		}
		System.out.print("\n");
		clientSocket.close();
	}
}
