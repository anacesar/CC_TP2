package Client;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import java.util.Scanner;

public class Client {

    public static void main(String[] args) throws UnknownHostException, IOException {
        Socket socket = new Socket(InetAddress.getLocalHost(), 8080);
        OutputStream out = socket.getOutputStream();
        Scanner scanner = new Scanner(System.in);
        String input = "";

        do{
            input = scanner.nextLine();
            out.write(input.getBytes());
            out.flush();
            
        }while(! input.equals("quit"));
        
        scanner.close();

    }
}
