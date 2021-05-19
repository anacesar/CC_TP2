package Common;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;


import Gateway.HttpGW;
import PDU.PDU;

public class UDPListener implements Runnable{
    private HttpGW gateway;
    private DatagramSocket udp_socket;
    private int udp_port;
    private InetAddress server;


    public UDPListener(HttpGW gateway, int my_port){
        this.gateway = gateway;
        this.udp_port = my_port;
    }



    public void run(){
        try{
            this.udp_socket = new DatagramSocket(udp_port);

            while(true){
                /* Receive a packet from FastFileServer */
                //System.out.println("waiting to receive pacotes in localip " + udp_socket.getLocalAddress().getHostAddress());
                byte[] message = new byte[2000];
                DatagramPacket receive = new DatagramPacket(message, message.length);
                udp_socket.receive(receive);
                System.out.println("received pacotes from source " + receive.getAddress().getHostAddress());

                /* PDU conversion*/
                PDU packet = PDU.fromBytes(message, message.length);
                packet.setInetAddress(receive.getAddress());
                packet.setPort(receive.getPort());
                System.out.println("pdu to string : " + packet.toString());
                System.out.println("address : " + packet.getInetAddress() + " and port : " + packet.getPort());

                /* send message to gateway */
                this.gateway.receive(packet);

                /*
                Thread.sleep(2000);
                this.udp_socket.send(receive);
                System.out.println("sended receive to someone");
                 */
                
            }

        }catch(IOException e){
            e.printStackTrace();
        }finally{
            udp_socket.close();
        }
    }     
}