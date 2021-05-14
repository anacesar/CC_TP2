package Common;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import Gateway.HttpGW;

public class UDPListener implements Runnable{
    private HttpGW gateway;
    private DatagramSocket udp_socket;
    private int udp_port;
    private InetAddress server

    public UDPListener(int my_port){
        this.gateway = gateway;
        this.udp_port = my_port;
        this.udp_socket = new DatagramSocket(udp_port);
    }

    public UDPListener(HttpGW gateway, int my_port){
        this.gateway = gateway;
        this.udp_port = my_port;
        this.udp_socket = new DatagramSocket(udp_port);
    }


    public void run(){
        try{

            while(true){
                /* Receive a packet from FastFileServer */
                byte[] message = new byte[2000];
                DatagramPacket receive = new DatagramPacket(message, message.length);
                udp_socket.receive(receive);
            }

        }catch(IOException e){
            e.printStackTrace();
        }finally{
            udp_socket.close();
        }
    }
       
        
    }

    
}