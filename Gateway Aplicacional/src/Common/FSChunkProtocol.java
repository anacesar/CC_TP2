package Common;

import PDU.PDU;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FSChunkProtocol implements Runnable{
    final long DATASIZE = 1448;
	
    /*private PDU packet; */
    private int port_answer;
    private InetAddress address;
    private PDU pdu;
    private List<PDU> answer_pdus;
    /* condition to block until pdu is received */
    private Lock lock;
    private Condition empty_pdus;

    //private Map<InetAddress, Integer> fastfileservers;
    private Map<Integer, InetAddress> fastfileservers;
    private Map<Integer, InetAddress> ffservers;
    private Map<Integer, PDU> datapdus;
    private DatagramSocket udp_socket;

    public FSChunkProtocol(PDU pdu, int port, InetAddress address){
        this.pdu = pdu;
        this.port_answer = port;
        this.address = address;
        this.answer_pdus = new ArrayList<PDU>();
        this.lock = new ReentrantLock();
        this.empty_pdus = this.lock.newCondition();
        this.fastfileservers = new HashMap<>();

    }

    public FSChunkProtocol(PDU pdu, HashMap<Integer, InetAddress> fastfileservers){
        this.pdu = pdu;
        this.answer_pdus = new ArrayList<PDU>();
        this.lock = new ReentrantLock();
        this.empty_pdus = this.lock.newCondition();

        this.fastfileservers = new HashMap<>(fastfileservers);
        this.ffservers = new HashMap<>(fastfileservers);
        this.datapdus = new HashMap<>();
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

    public int send(){
        try{
            byte[] message = this.pdu.toBytes();
            DatagramPacket packet_send = new DatagramPacket(message, message.length, address, port_answer);
            new DatagramSocket().send(packet_send);

            this.lock.lock();
            while(answer_pdus.size() == 0){ /* wait for ffserver response */
                //empty_pdus.await();
                if(!empty_pdus.await(2000, TimeUnit.MILLISECONDS)) return 1;
            }
            PDU pdu = answer_pdus.get(0);
            //System.out.println("!!!pdu received " + pdu);
        }catch(IOException | InterruptedException e){
            e.printStackTrace();
        }
        return 0;
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

    public void send(PDU pdu){
        try{
            byte[] message = pdu.toBytes();

            List<Integer> servers_port = new ArrayList<>(ffservers.keySet());
            int port = servers_port.get(0);
            DatagramPacket packet_send = new DatagramPacket(message, message.length, ffservers.get(port), port);
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


    public void ffsDown(int port){
        this.fastfileservers.remove(port);
    }


    public long waitforpdus(int limit) throws InterruptedException {
        int i = 0;
        long result = 0;
        this.lock.lock();
        while(i<limit){ /* while not all chunks are received */
            while(answer_pdus.size() == 0){
                empty_pdus.await();
            }

            PDU answer = answer_pdus.get(0);
            if(answer.getFlag() == 1 && answer.getType() == 1) {
                ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
                buffer.put(answer.getData());
                buffer.flip();
                long size = buffer.getLong();
                if(size == 0) { /* ffserver doesnt have file*/
                    fastfileservers.remove(answer.getPort());
                }else{ /* ffserver has file*/
                    result = size;
                }
            }
            answer_pdus.remove(answer);
            i++;
        }
        this.lock.unlock();
        return result;
    }


    public PDU dataPDU(String filename, int offset, long size, int nr_fragment) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES + 2 * Integer.BYTES + filename.length());
        buffer.put(ByteBuffer.allocate(4).putInt(offset).array());
        buffer.put(ByteBuffer.allocate(Long.BYTES).putLong(size).array());
        buffer.put(ByteBuffer.allocate(4).putInt(filename.length()).array());
        buffer.put(filename.getBytes());
        return new PDU(1, nr_fragment, this.pdu.getSeq_number(), buffer.array());

    }

    public void sendPDU(DatagramSocket udp_socket, PDU pdu, List<Integer> servers_port, int nr_server){
        InetAddress addr = fastfileservers.get(servers_port.get(nr_server));
        int port = servers_port.get(nr_server);
        send(udp_socket, pdu, addr, port);
    }


    public void fragmentPDU(DatagramSocket udp_socket, long filesize) throws InterruptedException {
        /* all ffservers that have file */
        int nr_servers = fastfileservers.size(), i=0;
        System.out.println("ready to fragment with nr servers " + nr_servers);

        String filename = new String(pdu.getData());
        List<Integer> servers_port = new ArrayList<>(fastfileservers.keySet());
        int total_fragments = 1;

        if(filesize > DATASIZE){
            /* needs fragmentation*/
            int rest = filesize % DATASIZE > 0 ? 1 : 0;
            total_fragments = Math.round(filesize / DATASIZE) + rest;
            new Thread(() -> {
                int nr_server, nr_fragment = 2, offset = 0, final_total_fragments = Math.round(filesize / DATASIZE) + rest;
                //System.out.println("Nr of fragments for file " + filename + " : " + total_fragments);
                while(nr_fragment <= final_total_fragments +2) {
                    nr_server = nr_fragment % nr_servers;
                    long size;
                    if(nr_fragment == final_total_fragments +2) size = filesize % DATASIZE;
                    else size = DATASIZE;

                    PDU pdu_offset = dataPDU(filename, offset, size, nr_fragment);
                    sendPDU(udp_socket, pdu_offset, servers_port, nr_server);
                    //System.out.println("protocol send chunk request ! nr_fragement : " + nr_fragment );
                    nr_fragment += 1;
                    offset += size;
                }
            }).start();
        }else{
            PDU pdu_offset = dataPDU(filename, 0, filesize, 2);
            sendPDU(udp_socket, pdu_offset, servers_port, 0);
        }

        /* wait for data pdus */
        int rest = filesize % DATASIZE > 0 ? 1 : 0;
        if(filesize>DATASIZE) total_fragments = Math.round(filesize/DATASIZE) + rest;
        System.out.println(total_fragments);
        this.lock.lock();
        while(i<total_fragments){ /* while not all chunks are received */
            while(answer_pdus.size() == 0){
                if(!empty_pdus.await(2000, TimeUnit.MILLISECONDS)){ /* timeout elapsed */
                    System.out.println("checking for lost pdus in request " + this.pdu.getSeq_number());
                    List<Integer> new_servers_port = new ArrayList<>(fastfileservers.keySet());
                    for(int nr_fragment = 2; nr_fragment<total_fragments+2; nr_fragment++){
                        if(!datapdus.containsKey(nr_fragment)){
                            long size;
                            if(nr_fragment == total_fragments +2) size = filesize % DATASIZE;
                            else size = DATASIZE;
                            int nr_server = nr_fragment % new_servers_port.size();
                            long offset = filesize - (total_fragments+1-nr_fragment)*DATASIZE - filesize%DATASIZE;
                            System.out.println("asking for pdu "  + nr_fragment + " with offset " + offset);
                            PDU pdu_offset = dataPDU(filename, (int)offset, size, nr_fragment);
                            sendPDU(udp_socket, pdu_offset, new_servers_port, nr_server);
                        }
                    }
                }
            }
            /* get pdu on list to consume */
            PDU answer = answer_pdus.get(0);
            if(!datapdus.containsKey(answer.getType())){
                System.out.println("adding chunk " + answer.getType() + " to map");
                datapdus.putIfAbsent(answer.getType(), answer);
                i++;
            }
            answer_pdus.remove(answer);
        }
        this.lock.unlock();
        Map.Entry<Integer, InetAddress> entry = fastfileservers.entrySet().iterator().next();
        send(udp_socket, new PDU(0,5, pdu.getSeq_number(), filename.getBytes()), entry.getValue(), entry.getKey());
    }

    public void broadcast_request(DatagramSocket udp_socket){
        try {
            fastfileservers.forEach(((port, inetAddress) -> {
                port_answer = port;
                address = inetAddress;
                send(udp_socket);
            }));

            /* wait for all ffservers to confirm */
            long filesize = waitforpdus(fastfileservers.size());
            System.out.println("file has " + filesize + " bytes");

            /* no one has file */
            if(filesize == 0 ){
                send(new PDU(0,8, pdu.getSeq_number()));
                System.out.println("no one is available for file download request");
            } else fragmentPDU(udp_socket, filesize);
        }catch(InterruptedException e){ e.printStackTrace(); }
    }

    public void run(){
        try {
            udp_socket = new DatagramSocket();

            /* check for message flag */
            switch(pdu.getFlag()) {
                case 0:
                    send(udp_socket);
                    break;
                case 1:
                    /* reconhece os ffs que têm o ficheiro e divide os pacotes */
                    broadcast_request(udp_socket);
                    break;
            }

            udp_socket.close();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }
}

