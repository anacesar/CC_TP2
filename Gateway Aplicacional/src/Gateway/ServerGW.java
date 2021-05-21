package Gateway;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;

import Common.TCPListener;
import Common.UDPListener;

public class ServerGW {
    static int udp_port = 8888;
    static int tcp_port = 8080;
    
    public static void main(String[] args) throws UnknownHostException {
        HttpGW gateway = new HttpGW();

        System.out.println("My ip " + InetAddress.getLocalHost().getHostAddress());
        
        new Thread(new UDPListener(gateway, udp_port)).start();
        new Thread(new TCPListener(gateway, tcp_port)).start();
        System.out.println("Gateway is listenning to TCP port " + tcp_port + " UDP port " + udp_port);

    }
}
