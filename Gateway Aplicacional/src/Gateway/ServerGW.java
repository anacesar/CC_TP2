package Gateway;

import java.net.SocketException;
import java.util.*;

import Common.TCPListener;
import Common.UDPListener;

public class ServerGW {
    static int udp_port = 8888;
    static int tcp_port = 8080;
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String input = "";
        /*
        while(!input.equals("HttpGW"))
            input = scanner.nextLine();
        */
        scanner.close();

        HttpGW gateway = new HttpGW();
        
        
        new Thread(new UDPListener(gateway, udp_port)).start();
        new Thread(new TCPListener(gateway, tcp_port)).start();
        System.out.println("Gateway is listenning to TCP port " + tcp_port + " UDP port " + udp_port);

    }
}
