package Common;

import PDU.PDU;

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
    private InetAddress address;
    private PDU pdu;
    private final List<PDU> answer_pdus;
    /* condition to block until pdu is received */
    private Lock lock;
    private Condition empty_pdus;


    public FSChunkProtocol(PDU pdu, int port, InetAddress address){
        this.pdu = pdu;
        this.port_answer = port;
        this.address = address;
        this.answer_pdus = new ArrayList<>();
        this.lock = new ReentrantLock();
        this.empty_pdus = this.lock.newCondition();
        
    }

    public void pdu(PDU pdu){
        this.lock.lock();
        answer_pdus.add(pdu);
        this.empty_pdus.signalAll();
        this.lock.unlock();
    }

    public void run(){
        try {
            DatagramSocket udp_socket = new DatagramSocket();

            /* check for message flag */
            switch(pdu.getFlag()){
                case 0:
                    byte[] message = pdu.toBytes();

                    DatagramPacket packet_send = new DatagramPacket(message, message.length, address, port_answer);
                    udp_socket.send(packet_send);
                    break;
                case 1:
                    /* reconhece os ffs que têm o ficheiro e divide os pacotes */
                    break;
            }


            //synchronized(answer_pdus){ wait();}
            /*
            while(answer_pdus.size()==0){
                condition.await();
            }*/
            /*
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


            ByteBuffer packet = ByteBuffer.allocate(8);
            packet.put(ByteBuffer.allocate(4).putInt(1234).array());
            packet.put(ByteBuffer.allocate(4).putInt(port_answer).array());

            byte[] message = packet.array();
            udp_socket.send(new DatagramPacket(message, message.length, address_answer, port_answer));
            System.out.println("Sending from protocol to ? : " + message);
            */


            udp_socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

