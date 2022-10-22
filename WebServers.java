/*
 * Eirini Kyriakou
 */
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.HashMap;

class WebServers{

    public static void main(String argv[]) throws Exception  {
        int myPort;
        String myIP;
        String firstServer;
        ArrayList<String> IPs = new ArrayList<String>();
        ArrayList<Integer> Ports = new ArrayList<Integer>();
        HashMap<String, String> clientsData = new HashMap<String, String>();

        try{
            URL ip = new URL("https://checkip.amazonaws.com");
            BufferedReader br = new BufferedReader(new InputStreamReader(ip.openStream()));
            myIP = br.readLine();
        }catch(Exception e){
            Scanner scanner = new Scanner(System.in);
            System.out.println("Enter your IP:");
            myIP = scanner.nextLine();
            scanner.close();
        }
        System.out.println("IP Address = " + myIP);
        
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter port:");
        myPort = scanner.nextInt(); 
        scanner.nextLine();
        System.out.println("Port = " + myPort);  

        System.out.print("Are you the first Server? (Answer 'yes' or 'no'): ");
        firstServer = scanner.nextLine();
        //System.out.println("Your answer is: " + firstServer);  
        
        if (firstServer.equals("no")){
            System.out.print("Enter IP you want to connect: ");
            String IP = scanner.nextLine();  
            //System.out.println("IP is: " + IP); 

            System.out.println("Enter port you want to connect:");
            int Port = scanner.nextInt();  
            //scanner.nextLine();
            //System.out.println("Port is: " + Port);

		    Socket clientSocket = new Socket(IP, Port);
            DataOutputStream outToServer =  new DataOutputStream(clientSocket.getOutputStream());
            BufferedReader inFromServer =  new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); 

		    String sentence = "Server "+ myIP + " " + myPort + " join to " + IP + " " + Port ; 
		    outToServer.writeBytes(sentence + '\n');

            String answerFromServer = inFromServer.readLine(); 
		    System.out.println("\nFROM SERVER: " + answerFromServer);
            StringTokenizer tokenizedLine = new StringTokenizer(answerFromServer);
            //String token = tokenizedLine.nextToken();
            while ( tokenizedLine.hasMoreTokens() /*true*/) {
                IPs.add(tokenizedLine.nextToken());
                Ports.add(Integer.parseInt(tokenizedLine.nextToken()));
            }
            outToServer.close();
            clientSocket.close();
        }else{
            IPs.add(myIP);
            Ports.add(myPort);
        }
        scanner.close();
        System.out.println("\nIPS: "+ IPs);
        System.out.println("Ports: "+ Ports + "\n");

        ServerSocket listenSocket = new ServerSocket(myPort);
        System.out.println("Server ready on " + myPort);

        Thread perdiodic = new Thread(new Periodic(myIP,myPort,IPs,Ports));
        perdiodic.start();
    
        System.out.println("Waiting for connection...");
        while (true) {
            Socket connectionSocket = listenSocket.accept();
			//System.out.println("Connected!");

            BufferedReader in = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
            String requestMessageLine = in.readLine();

            String token="";
            if(requestMessageLine!=null){
                StringTokenizer tokenizedLine = new StringTokenizer(requestMessageLine);
                token = tokenizedLine.nextToken();
            }

            if (token.equals("Server")){
                //System.out.println("Message from Server.");
                Thread workerServer = new Thread(new ServerWorker(connectionSocket,requestMessageLine,myIP,myPort,IPs,Ports,clientsData));
                workerServer.start();
            }  //End of Server
            else { //Beggin of Client
                //System.out.println("Message from Client.");
                Thread workerClient = new Thread(new ClientWorker(connectionSocket,requestMessageLine,myIP,myPort,IPs,Ports,clientsData));
                workerClient.start();
            }
           // System.out.println("\nWaiting for another connection...");
        }
        //listenSocket.close();
    }
}

class ServerWorker implements Runnable {
    private final Socket connectionSocket;
    private String in;
    private String myIP; 
    private int myPort;
    private ArrayList<String> IPs; 
    private ArrayList<Integer> Ports;
    private HashMap<String, String> clientsData;

    public ServerWorker(Socket socket, String in, String myIP, int myPort, ArrayList<String> IPs, ArrayList<Integer> Ports, HashMap<String, String> clientsData){
        this.connectionSocket = socket;
        this.in = in;
        this.myIP = myIP; 
        this.myPort = myPort;
        this.IPs = IPs; 
        this.Ports = Ports;
        this.clientsData = clientsData;
    }

    @Override
    public void run(){
        String requestMessageLine = in;
        //System.out.println("\nFROM SERVER: " + requestMessageLine);
        StringTokenizer tokenizedLine = new StringTokenizer(requestMessageLine);
        tokenizedLine.nextToken(); //Server
        String IP = tokenizedLine.nextToken(); //from whom
        int Port = Integer.parseInt(tokenizedLine.nextToken());
        String token = tokenizedLine.nextToken(); //what request
        if(token.equals("join")){
            System.out.println("\nFROM SERVER: " + requestMessageLine);
            tokenizedLine.nextToken();
            String toIP = tokenizedLine.nextToken(); //In whom it was send originally
            int toPort = Integer.parseInt(tokenizedLine.nextToken());

            if (toIP.equals(myIP) && toPort==myPort){ //If I'm the original sender I reply to the new server
                String answer = "";
                for (int i = 0; i < IPs.size(); i++) {
                    //System.out.println(cars.get(i));
                    if(i == IPs.size()-1){
                        answer += IPs.get(i) + " " + Ports.get(i) + " ";
                    }else{
                        answer += IPs.get(i) + " " + Ports.get(i) + " ";
                    }
                }
                answer += IP + " " + Port + "\n";
                try {
                    ServerToClient(connectionSocket,IP,Port,answer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            for (int i = 0; i < IPs.size(); i++) {                
                if((IPs.get(i).equals(myIP)) && (Ports.get(i)==myPort) && (IPs.size()>1)){
                    if (i == IPs.size()-1){ //I'm the last IP in the list
                        if(IPs.get(0).equals(toIP) && Ports.get(0)==toPort){
                            //System.out.println("\t\t\tEnd of cycle");
                            break;
                        }else{
                            try {
                                ClientToServer(IPs.get(0), Ports.get(0), requestMessageLine);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }else{ //I'm not the last IP in the list
                        if(IPs.get(i+1).equals(toIP) && Ports.get(i+1)==toPort){
                            //System.out.println("\t\t\tEnd of cycle");
                            break;
                        }else{
                            try {
                                ClientToServer(IPs.get(i+1), Ports.get(i+1), requestMessageLine);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    break;
                }
            }

            IPs.add(IP);
            Ports.add(Port);
            System.out.println("\nIPS: "+ IPs);
            System.out.println("Ports: "+ Ports);
            System.out.println("\nEnd of thread.\nWaiting for connection in main...");
        } ///End of join
        else if(token.equals("remove")){ 
            System.out.println("\nFROM SERVER: " + requestMessageLine);
            String rIP = tokenizedLine.nextToken(); //remove element
            int rPort = Integer.parseInt(tokenizedLine.nextToken());
            System.out.println("Server we want to remove: " + rIP);
            for (int i = 0; i < IPs.size(); i++) {                
                if((IPs.get(i).equals(rIP)) && (Ports.get(i)==rPort)){
                    IPs.remove(i);
                    Ports.remove(i);
                    System.out.println("Server was sucesfully removed.");
                    break;
                }
            }

            if(IPs.size() > 2 ){
                for (int i = 0; i < IPs.size(); i++){ //case for if originally there were more than 3 Servers.
                    //if(i+1 < IPs.size()){
                        //if(IPs.get(i+1).equals(IP) && Ports.get(i+1)==Port) break;
                    //}
                    if((IPs.get(i).equals(myIP)) && (Ports.get(i)==myPort) && (IPs.size()>1)){
                        if (i == IPs.size()-1){ //I'm the last IP in the list
                            if(IP.equals(IPs.get(0)) && Port==Ports.get(0)) break;
                            try {
                                ClientToServer(IPs.get(0), Ports.get(0), requestMessageLine);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }else{ //I'm not the last IP in the list
                            if(IP.equals(IPs.get(i+1)) && Port==Ports.get(i+1)) break;
                            try {
                                ClientToServer(IPs.get(i+1), Ports.get(i+1), requestMessageLine);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            
                        }
                        break;
                    }
                }
            }
            System.out.println("\nIPS: "+ IPs);
            System.out.println("Ports: "+ Ports);
            System.out.println("\nEnd of thread.\nWaiting for connection in main...");
        } //End of remove
        else if(token.equals("GET")){
            System.out.println("\nFROM SERVER: " + requestMessageLine);
            String key = tokenizedLine.nextToken();
            String token1 = tokenizedLine.nextToken(); //"HTTP/1.1" or value
            if (!token1.equals("HTTP/1.1")){
                clientsData.put(key,token1);
            }
            String toIP = IP; //In whom it was send originally
            int toPort = Port;
            if(!((toIP.equals(myIP)) && (toPort==myPort))){
                if(clientsData.get(key)!=null){ //i have the key
                    for (int i = 0; i < IPs.size(); i++) {                
                        if((IPs.get(i).equals(myIP)) && (Ports.get(i)==myPort) && (IPs.size()>1)){
                            if (i == IPs.size()-1){ //I'm the last IP in the list
                                try {
                                    ClientToServer(IPs.get(0), Ports.get(0), "Server " + toIP + " " + toPort +" GET " + key + " " + clientsData.get(key) +" HTTP/1.1");
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }else{ //I'm not the last IP in the list
                                try {
                                    ClientToServer(IPs.get(i+1), Ports.get(i+1), "Server " + toIP + " " + toPort +" GET " + key + " " + clientsData.get(key) +" HTTP/1.1");
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            break;
                        }
                    }
                }else{
                    for (int i = 0; i < IPs.size(); i++) {                
                        if((IPs.get(i).equals(myIP)) && (Ports.get(i)==myPort) && (IPs.size()>1)){
                            if (i == IPs.size()-1){ //I'm the last IP in the list
                                try {
                                    ClientToServer(IPs.get(0), Ports.get(0), "Server "  + toIP + " " + toPort + " GET " + key +" HTTP/1.1");
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }else{ //I'm not the last IP in the list
                                try {
                                    ClientToServer(IPs.get(i+1), Ports.get(i+1), "Server "  + toIP + " " + toPort + " GET " + key +" HTTP/1.1");
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }

        //System.out.println("\nEnd of thread.\nWaiting for connection in main...");
    }

    static void ServerToClient(Socket connectionSocket, String IP, int Port, String msg) throws IOException{
        //System.out.println("Mphke sto ServerToClient");
        DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
        //BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
        //System.out.println("\nMessage about to send to " + IP + " " + Port + ":\n\t" + msg);
        outToClient.writeBytes(msg);
        outToClient.close();
        connectionSocket.close();
        //System.out.println("Message was Send");
        System.out.println("Connection closed");
    }

    static void ClientToServer(String IP, int Port, String msg) throws IOException{
        //System.out.println("Mphke sto ClientToServer");
        Socket clientSocket = new Socket(IP, Port);
        DataOutputStream outToServer =  new DataOutputStream(clientSocket.getOutputStream());
		//BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        //System.out.println("\nMessage about to send to " + IP + " " + Port + ":\n\t" + msg);
        outToServer.writeBytes(msg);
        outToServer.close();
        clientSocket.close();
        //System.out.println("Message was Send");
        System.out.println("Connection closed");
    }
}

class Periodic implements Runnable{
    private String myIP; 
    private int myPort;
    private ArrayList<String> IPs; 
    private ArrayList<Integer> Ports;

    public Periodic(String myIP, int myPort, ArrayList<String> IPs, ArrayList<Integer> Ports){
        this.myIP = myIP; 
        this.myPort = myPort;
        this.IPs = IPs; 
        this.Ports = Ports;
    }

    @Override
    public void run() {
        while(true){
            try {
                if((IPs.size()>1)){
                    for (int i = 0; i < IPs.size(); i++) {
                        if((IPs.get(i).equals(myIP)) && (Ports.get(i)==myPort)){
                            //System.out.println("\tMphke");
                            if (i == IPs.size()-1){ //I'm the last IP in the list
                                try {
                                    ClientToServer(IPs.get(0), Ports.get(0), "Server "+ myIP + " " + myPort +" health check\n");
                                } catch (IOException e) {
                                    System.out.println("Server " + IPs.get(0) + " " + Ports.get(0) + " is out");
                                    if(IPs.size()>2){
                                        try {
                                            ClientToServer(IPs.get(1), Ports.get(1), "Server "+ myIP + " " + myPort +" remove "+ IPs.get(0) + " " + Ports.get(0)+"\n");
                                        } catch (IOException e1) {
                                            e1.printStackTrace();
                                        }
                                    }
                                    IPs.remove(0);
                                    Ports.remove(0);
                                    System.out.println("\nIPS: "+ IPs);
                                    System.out.println("Ports: "+ Ports);
                                }
                            }else{ //I'm not the last IP in the list
                                try {
                                    ClientToServer(IPs.get(i+1), Ports.get(i+1), "Server "+ myIP + " " + myPort +" health check\n");
                                } catch (IOException e) {
                                    System.out.println("Server " + IPs.get(i+1) + " " + Ports.get(i+1) + " is out");
                                    if(IPs.size()>2){
                                        if(i+2 < IPs.size()){
                                            try {
                                                ClientToServer(IPs.get(i+2), Ports.get(i+2), "Server "+ myIP + " " + myPort +" remove "+ IPs.get(i+1) + " " + Ports.get(i+1)+"\n");
                                            } catch (IOException e1) {
                                                e1.printStackTrace();
                                            }
                                        }else{
                                            try {
                                                ClientToServer(IPs.get(0), Ports.get(0), "Server "+ myIP + " " + myPort +" remove "+ IPs.get(i+1) + " " + Ports.get(i+1)+"\n");
                                            } catch (IOException e1) {
                                                e1.printStackTrace();
                                            }
                                        }
                                    }
                                    IPs.remove(i+1);
                                    Ports.remove(i+1);
                                    System.out.println("\nIPS: "+ IPs);
                                    System.out.println("Ports: "+ Ports);
                                }
                                
                            }
                            break;
                        }
                    }
                }
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    static void ClientToServer(String IP, int Port, String msg) throws IOException{
        //System.out.println("Mphke sto ClientToServer");
        Socket clientSocket = new Socket(IP, Port);
        DataOutputStream outToServer =  new DataOutputStream(clientSocket.getOutputStream());
		//BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        //System.out.println("\nHealth Check to " + IP + " " + Port);
        outToServer.writeBytes(msg);
        outToServer.close();
        clientSocket.close();
        //System.out.println("Message was Send");
        //System.out.println("Connection closed");
    }
    
}

class ClientWorker implements Runnable {
    private final Socket connectionSocket;
    private String in;
    private String myIP; 
    private int myPort;
    private ArrayList<String> IPs; 
    private ArrayList<Integer> Ports;
    private HashMap<String, String> clientsData;

    public ClientWorker(Socket socket, String in, String myIP, int myPort, ArrayList<String> IPs, ArrayList<Integer> Ports, HashMap<String, String> clientsData){
        this.connectionSocket = socket;
        this.in = in;
        this.myIP = myIP; 
        this.myPort = myPort;
        this.IPs = IPs; 
        this.Ports = Ports;
        this.clientsData = clientsData;
    }

    @Override
    public void run(){
        String requestMessageLine = in;
        System.out.println("\nFROM CLIENT: " + requestMessageLine);
        StringTokenizer tokenizedLine = new StringTokenizer(requestMessageLine);
        tokenizedLine.nextToken(); //Client
        String request = tokenizedLine.nextToken(); //GET or PUT
        if(request.equals("PUT")){ 
            String key = tokenizedLine.nextToken(); 
            String value = tokenizedLine.nextToken(); 
            tokenizedLine.nextToken(); //HTTP/1.1
            tokenizedLine.nextToken(); //to
            String toIP = tokenizedLine.nextToken(); //In whom it was send originally
            int toPort = Integer.parseInt(tokenizedLine.nextToken());
            clientsData.put(key,value);
            for (int i = 0; i < IPs.size(); i++) {                
                if((IPs.get(i).equals(myIP)) && (Ports.get(i)==myPort) && (IPs.size()>1)){
                    if (i == IPs.size()-1){ //I'm the last IP in the list
                        if(IPs.get(0).equals(toIP) && Ports.get(0)==toPort){
                            //System.out.println("\t\t\tEnd of cycle");
                            break;
                        }else{
                            try {
                                ClientToServer(IPs.get(0), Ports.get(0), requestMessageLine);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }else{ //I'm not the last IP in the list
                        if(IPs.get(i+1).equals(toIP) && Ports.get(i+1)==toPort){
                            //System.out.println("\t\t\tEnd of cycle");
                            break;
                        }else{
                            try {
                                ClientToServer(IPs.get(i+1), Ports.get(i+1), requestMessageLine);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    break;
                }
            }
            if((toIP.equals(myIP)) && (toPort==myPort)){
                try {
                    ServerToClient(connectionSocket, "Success!\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("\n"+clientsData);
        } //End of PUT
        else if(request.equals("GET")){
            String key = tokenizedLine.nextToken();
            String token = tokenizedLine.nextToken(); //"HTTP/1.1" or value
            if (!token.equals("HTTP/1.1")){
                clientsData.put(key,token);
                tokenizedLine.nextToken(); //HTTP/1.1
            }
            tokenizedLine.nextToken(); //to
            String toIP = tokenizedLine.nextToken(); //In whom it was send originally
            int toPort = Integer.parseInt(tokenizedLine.nextToken());

            if(clientsData.get(key)!=null){
                try {
                    ServerToClient(connectionSocket, "Success! {" + key + " = " + clientsData.get(key) + "}\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else{
                for (int i = 0; i < IPs.size(); i++) {                
                    if((IPs.get(i).equals(myIP)) && (Ports.get(i)==myPort) && (IPs.size()>1)){
                        if (i == IPs.size()-1){ //I'm the last IP in the list
                            try {
                                //ClientToServer(IPs.get(0), Ports.get(0), requestMessageLine);
                                ClientToServer(IPs.get(0), Ports.get(0), "Server "  + toIP + " " + toPort + " GET " + key +" HTTP/1.1");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }else{ //I'm not the last IP in the list
                            try {
                                //ClientToServer(IPs.get(i+1), Ports.get(i+1), requestMessageLine);
                                ClientToServer(IPs.get(i+1), Ports.get(i+1), "Server "  + toIP + " " + toPort + " GET " + key +" HTTP/1.1");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    }
                }
                while(clientsData.get(key)==null){}
                try {
                    ServerToClient(connectionSocket, "Success! {" + key + " = " + clientsData.get(key) + "}\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        System.out.println("\nEnd of thread.\nWaiting for connection in main...");
    }

    static void ClientToServer(String IP, int Port, String msg) throws IOException{
        //System.out.println("Mphke sto ClientToServer");
        Socket clientSocket = new Socket(IP, Port);
        DataOutputStream outToServer =  new DataOutputStream(clientSocket.getOutputStream());
		//BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        //System.out.println("\nMessage about to send to " + IP + " " + Port + ":\n\t" + msg);
        outToServer.writeBytes(msg);
        outToServer.close();
        clientSocket.close();
        //System.out.println("Message was Send");
        System.out.println("Connection closed");
    }

    static void ServerToClient(Socket connectionSocket, String msg) throws IOException{
        //System.out.println("Mphke sto ServerToClient");
        DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
        //BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
        //System.out.println("\nMessage about to send:\n\t" + msg);
        outToClient.writeBytes(msg);
        outToClient.close();
        connectionSocket.close();
        //System.out.println("Message was Send");
        System.out.println("Connection closed");
    }
}