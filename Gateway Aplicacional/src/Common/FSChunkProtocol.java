package Common;

import FastFileServer.FastFileSrv;
import PDU.PDU;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FSChunkProtocol implements Runnable{
    final long DATASIZE = 1448;
	
    /*private PDU packet; */
    private int port_answer;
    private InetAddress address;
    private PDU pdu;
    private final List<PDU> answer_pdus;
    /* condition to block until pdu is received */
    private Lock lock;
    private Condition empty_pdus;

    //private Map<InetAddress, Integer> fastfileservers;
    private Map<Integer, InetAddress> fastfileservers;

    public FSChunkProtocol(PDU pdu, int port, InetAddress address){
        this.pdu = pdu;
        this.port_answer = port;
        this.address = address;
        this.answer_pdus = new CopyOnWriteArrayList<>();
        this.lock = new ReentrantLock();
        this.empty_pdus = this.lock.newCondition();
        
    }

    public FSChunkProtocol(PDU pdu, HashMap<Integer, InetAddress> fastfileservers){
        this.pdu = pdu;
        this.answer_pdus = new ArrayList<>();
        this.lock = new ReentrantLock();
        this.empty_pdus = this.lock.newCondition();

        this.fastfileservers = fastfileservers;
    }


    public void send(DatagramSocket udp_socket){
        try{
            byte[] message = pdu.toBytes();
            DatagramPacket packet_send = new DatagramPacket(message, message.length, address, port_answer);
            udp_socket.send(packet_send);
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public void send(DatagramSocket udp_socket, PDU pdu, InetAddress address, int port){
        try{
            byte[] message = pdu.toBytes();
            DatagramPacket packet_send = new DatagramPacket(message, message.length, address, port);
            udp_socket.send(packet_send);
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public void pdu(PDU pdu){
        this.lock.lock();
        answer_pdus.add(pdu);
        this.empty_pdus.signal();
        this.lock.unlock();
    }


    public long waitforpdus(int limit) throws InterruptedException {
        int i = 0;
        this.lock.lock();
        long result = 0;
        while(i<limit){ /* while not all chunks are received */
            System.out.println("waiting for transfer pdus...");
            while(answer_pdus.size() == 0){ empty_pdus.await();}

            System.out.println("pdu received " + answer_pdus.get(0).toString());
            PDU answer = answer_pdus.get(0);
            if(answer.getFlag() == 1 && answer.getType() == 1) {
                ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
                buffer.put(answer.getData());
                buffer.flip();
                long size = buffer.getLong();
                if(size == 0) { /* ffserver doesnt have file*/
                    fastfileservers.remove(answer.getPort());
                }else{ /* ffserver has file*/
                    System.out.println("File has length " + size);
                    result = size;
                }
                answer_pdus.remove(answer);
            }
            i++;
        }
        this.lock.unlock();
        return result;
    }

    public void fragmentPDU(DatagramSocket udp_socket, long filesize) throws InterruptedException {
        /* all ffservers that have file */
        int nr_servers = fastfileservers.size();
        System.out.println("ready to fragment with nr servers " + nr_servers);

        String filename = new String(pdu.getData());

        int total_fragments = 1;
        if(filesize > DATASIZE){
            /* needs fragmentation*/
            int nr_fragment = 2, offset =0; total_fragments = (int)filesize/(int)DATASIZE;
            List<Integer> servers_port = new ArrayList<>(fastfileservers.keySet());
            System.out.println("Nr of fragments for file " + filename + " : " + total_fragments);

            while(nr_fragment <= total_fragments +1){
                int nr_server = nr_fragment % nr_servers;
                long size;
                if(nr_fragment == total_fragments+1) size = filesize - (nr_fragment-1) * DATASIZE;
                else size = DATASIZE;

                ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES + 2*Integer.BYTES + filename.length());
                buffer.put(ByteBuffer.allocate(4).putInt(offset).array());
                buffer.put(ByteBuffer.allocate(Long.BYTES).putLong(size).array());
                buffer.put(ByteBuffer.allocate(4).putInt(filename.length()).array());
                buffer.put(filename.getBytes());
                PDU pdu_offset = new PDU(1, nr_fragment , this.pdu.getSeq_number(), buffer.array());

                InetAddress addr = fastfileservers.get(servers_port.get(nr_server));
                int port = servers_port.get(nr_server);
                System.out.println("server " + nr_server + " ip : " + addr + " port : " + port);
                send(udp_socket, pdu_offset, addr, port);
                System.out.println("protocol send chunk request ! nr_fragement : " + nr_fragment + " and datasize : " + size );
                nr_fragment++;
                offset += size;
            }
        }

        waitforpdus(total_fragments);
        Map.Entry<Integer, InetAddress> entry = fastfileservers.entrySet().iterator().next();
        send(udp_socket, new PDU(0,5, pdu.getSeq_number(), filename.getBytes()), entry.getValue(), entry.getKey());
    }

    public void broadcast_request(DatagramSocket udp_socket) throws InterruptedException {
        System.out.println("nr of fastfileservers " + fastfileservers.size());
        fastfileservers.forEach(((port, inetAddress) -> {
            port_answer = port; address = inetAddress;
            send(udp_socket);
        }));

        /* wait for all ffservers to confirm */
        long filesize = waitforpdus(fastfileservers.size());

        /* no one has file */
        if(fastfileservers.size() == 0 ) ;
        else fragmentPDU(udp_socket, filesize);

    }



    public void run(){
        try {
            DatagramSocket udp_socket = new DatagramSocket();

            /* check for message flag */
            switch(pdu.getFlag()){
                case 0:
                    send(udp_socket);
                    break;
                case 1:
                    /* reconhece os ffs que têm o ficheiro e divide os pacotes */
                    broadcast_request(udp_socket);
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

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}

