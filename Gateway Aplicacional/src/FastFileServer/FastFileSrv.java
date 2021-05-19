package FastFileServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Random;

import Common.Global;
import PDU.PDU;

public class FastFileSrv {
    private int port;
    private InetAddress server_address;
    private int my_port;
    private InetAddress ffs_address;
    private String files_path;
    private DatagramSocket udp_socket;

    public FastFileSrv(int port, InetAddress sAddress, String files_path) throws SocketException{
        this.port = port;
        this.server_address = sAddress;
        this.files_path = files_path;
        this.my_port = new Random().nextInt((2000 - 1000) + 1) + 1000;
        this.udp_socket = new DatagramSocket(my_port);
        this.ffs_address = this.udp_socket.getLocalAddress();
        System.out.println("ffs address " + ffs_address);
        //udp_socket.bind(this.port);
    }



    public void run(){

        /* listening thread */
        new Thread(() -> {
            //this.udp_socket.connect(server_address, port);
            try{
                while(true){
                    /* Receive a packet from gateway(via protocol) */
                    byte[] message = new byte[2000];
                    DatagramPacket receive = new DatagramPacket(message, message.length);
                    udp_socket.receive(receive);

                    System.out.println("ffs received : " + new String(message));
                    
                    /* check pdu flag/type and do stuff */
                }
            }catch(IOException e){e.printStackTrace();}
        }).start();
        

        /* ask for authentication */
        try{
            PDU pdu = new PDU() {}

            ByteBuffer packet = ByteBuffer.allocate(8);
            packet.put(ByteBuffer.allocate(Integer.SIZE/8).putInt(1234).array());
            packet.put(ByteBuffer.allocate(4).putInt(my_port).array());

            byte[] message = packet.array();
            //System.out.println("message : " + Global.byteArraytoHexString(message));
            System.out.println("message2 : " + packet.toString());


            DatagramPacket packet_send = new DatagramPacket(message, message.length, server_address, port);
            udp_socket.send(packet_send);
            System.out.println("Packet with dest ip " + packet_send.getAddress());
            System.out.println("Sending from ffs to gw : " + Global.byteArraytoHexString(message));
        }catch(IOException e) {
            e.printStackTrace();
        }
    }

}
