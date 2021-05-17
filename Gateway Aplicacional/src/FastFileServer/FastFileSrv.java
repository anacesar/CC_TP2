package FastFileServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;

import Common.Global;

public class FastFileSrv {
    private int port;
    private InetAddress server_address;
    private String files_path;
    private DatagramSocket udp_socket;

    public FastFileSrv(int port, InetAddress sAddress, String files_path) throws SocketException{
        this.port = port;
        this.server_address = sAddress;
        this.files_path = files_path;
        /* conects to socket */
        System.out.println("Trying to conect to udp socket in port : " + port);
        this.udp_socket = new DatagramSocket();
        //udp_socket.bind(this.port);
    }



    public void run(){

        /* listening thread */
        new Thread(() -> {
            try{
                while(true){
                    /* Receive a packet from gateway(via protocol) */
                    byte[] message = new byte[2000];
                    DatagramPacket receive = new DatagramPacket(message, message.length);
                    udp_socket.receive(receive);

                    System.out.println("ffs received : " + new String(message, "ASCII"));
                    
                    /* check pdu flag/type and do stuff */
                }
            }catch(IOException e){e.printStackTrace();}
        }).start();
        

        /* ask for authentication */
        try{


            ByteBuffer packet = ByteBuffer.allocate(8);
            packet.put(ByteBuffer.allocate(Integer.SIZE/8).putInt(1234).array());
            packet.put(ByteBuffer.allocate(4).putInt(port).array());

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
