package FastFileServer;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.Scanner;



public class FastFileServer {
    public static void main(String[] args) throws UnknownHostException {
        if (args.length < 2) {
            System.out.println("Syntax: FastFileSrv <hostname> <port>");
            return;
        }
        /* get server address and port */
        InetAddress server_address = InetAddress.getByName(args[0]);
        int port = Integer.parseInt(args[1]);
        System.out.println("My ip " + InetAddress.getLocalHost().getHostAddress());

        /* ask for directory to share */
        String path; 
        if(args.length > 2) path = args[2];
        else{
            System.out.println("Please enter name of directory to share: ");
            path = new Scanner(System.in).nextLine();
        }

        try{
            FastFileSrv ffserver = new FastFileSrv(port, server_address, path);
            ffserver.run();
        }catch(SocketException | InterruptedException e){
            e.printStackTrace();
            System.out.println("An error ocurred trying to open Socket!! ");
        }
    }
}
