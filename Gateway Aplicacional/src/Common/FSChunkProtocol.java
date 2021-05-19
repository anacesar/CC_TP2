package Common;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FSChunkProtocol implements Runnable{
	
    /*private PDU packet; */
    private int port_answer;
    private InetAddress source_address;
    private int port_redirect;
    private InetAddress answer_address;
    private byte[] message;
    private final List<byte[]> answer_pdus;
    /* condition to block until pdu is received */
    private Lock lock;
    private Condition empty_pdus;


    public FSChunkProtocol(byte[] message, int port, InetAddress source_address, InetAddress answer_address){
        this.message = message;
        this.port_answer = port;
        this.source_address = source_address;
        this.answer_address = answer_address;
        System.out.println("Source address : " + source_address + " answer address "  + answer_address);
        this.answer_pdus = new ArrayList<>();
        this.lock = new ReentrantLock();
        this.empty_pdus = this.lock.newCondition();
        
    }

    public void pdu(byte[] message){
        this.lock.lock();
        answer_pdus.add(message);
        this.empty_pdus.signalAll();
        this.lock.unlock();
    }

    public void run(){
        try {
            DatagramSocket udp_socket = new DatagramSocket();

            /* connect socket to ffs ip and port */

            System.out.println("Protocol ready to control message : " + new String(message));


            int seq_Number = ByteBuffer.wrap(Arrays.copyOfRange(message, 0, 4)).getInt();
            int port = ByteBuffer.wrap(Arrays.copyOfRange(message, 4, 8)).getInt();

            /* check for message flag */

            System.out.println("protocol received message seq_number " + seq_Number + " and port " + port);

            //synchronized(answer_pdus){ wait();}
            /*
            while(answer_pdus.size()==0){
                condition.await();
            }*/

            this.lock.lock();
            while(answer_pdus.size()==0){
                this.empty_pdus.await();
            }
            this.lock.unlock();

            System.out.println("protocol awake!! new message to nr seq "+ seq_Number);

            byte[] message2 = answer_pdus.get(0);
            int seq_Number2 = ByteBuffer.wrap(Arrays.copyOfRange(message, 0, 4)).getInt();
            int port2 = ByteBuffer.wrap(Arrays.copyOfRange(message, 4, 8)).getInt();

            System.out.println("2 ) protocol received message seq_number " + seq_Number2 + " and port " + port2);

            udp_socket.send(new DatagramPacket(message2, message2.length, source_address, port2));
            System.out.println("protocol sending message to ip " + source_address.getHostAddress() + " and port " + port2);

            /*
            ByteBuffer packet = ByteBuffer.allocate(8);
            packet.put(ByteBuffer.allocate(4).putInt(1234).array());
            packet.put(ByteBuffer.allocate(4).putInt(port_answer).array());

            byte[] message = packet.array();
            udp_socket.send(new DatagramPacket(message, message.length, address_answer, port_answer));
            System.out.println("Sending from protocol to ? : " + message);
            */

            udp_socket.close();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}

