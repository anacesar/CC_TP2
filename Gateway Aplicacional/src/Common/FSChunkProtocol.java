package Common;

import PDU.PDU;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
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
    private Map<Integer, Map<Integer,PDU>> datapdus;

    public FSChunkProtocol(PDU pdu, int port, InetAddress address){
        this.pdu = pdu;
        this.port_answer = port;
        this.address = address;
        this.answer_pdus = new ArrayList<PDU>();
        this.lock = new ReentrantLock();
        this.empty_pdus = this.lock.newCondition();

    }

    public FSChunkProtocol(PDU pdu, HashMap<Integer, InetAddress> fastfileservers){
        this.pdu = pdu;
        this.answer_pdus = new ArrayList<PDU>();
        this.lock = new ReentrantLock();
        this.empty_pdus = this.lock.newCondition();

        this.fastfileservers = fastfileservers;
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


    public void checklostPDUs(int limit, int seq_number){
        System.out.println("checking for lost pdus ");
        Map<Integer, PDU> pdus_received = datapdus.get(seq_number);
        for(int i = 2; i<limit+2; i++){
            if(!pdus_received.containsKey(i)){
                System.out.println("resending request for pdu "  + i);
                //PDU pdu = dataPDU(filename, )
                //int random = new Random().nextInt(datapdus.size()-1);

            }
        }
    }


    public long waitforpdus(int limit, int seq_number) throws InterruptedException {
        System.out.println("limit : " + limit);
        int i = 0;
        long result = 0;
        this.lock.lock();
        while(i<limit){ /* while not all chunks are received */
            System.out.println("waiting for transfer pdus...");
            while(answer_pdus.size() == 0){
                //if(i!=0) checklostPDUs(limit, seq_number);
                empty_pdus.await(3000, TimeUnit.MILLISECONDS);
            }
            System.out.println("awake");

            PDU answer = answer_pdus.get(0);
            System.out.println("pdu received " + answer.toString());
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
            }else{
                System.out.println("adding chunk " + answer.getType() + " to map");
                if(datapdus.get(answer.getSeq_number())==null) datapdus.put(answer.getSeq_number(), new HashMap<>());
                datapdus.get(answer.getSeq_number()).putIfAbsent(answer.getType(),answer);

            }
            answer_pdus.remove(answer);
            i++;
        }
        System.out.println("size of answers pdus " + answer_pdus.size());
        this.lock.unlock();
        return result;
    }


    public PDU dataPDU(String filename, int offset, long size, int nr_fragment){
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES + 2*Integer.BYTES + filename.length());
        buffer.put(ByteBuffer.allocate(4).putInt(offset).array());
        buffer.put(ByteBuffer.allocate(Long.BYTES).putLong(size).array());
        buffer.put(ByteBuffer.allocate(4).putInt(filename.length()).array());
        buffer.put(filename.getBytes());
        return new PDU(1, nr_fragment , this.pdu.getSeq_number(), buffer.array());

        //System.out.println("server " + nr_server + " ip : " + addr + " port : " + port);
    }

    public void sendPDU(DatagramSocket udp_socket, PDU pdu, List<Integer> servers_port, int nr_server){
        InetAddress addr = fastfileservers.get(servers_port.get(nr_server));
        int port = servers_port.get(nr_server);
        send(udp_socket, pdu, addr, port);
    }

    public void fragmentPDU(DatagramSocket udp_socket, long filesize) throws InterruptedException {
        /* all ffservers that have file */
        int nr_servers = fastfileservers.size();
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

        waitforpdus(total_fragments, pdu.getSeq_number());


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
        long filesize = waitforpdus(fastfileservers.size(), -1);

        /* no one has file */
        if(fastfileservers.size() == 0 ) throw new InterruptedException("No Fast File Server has file! ");
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
                    try{
                        broadcast_request(udp_socket);}
                    catch(InterruptedException e){}
                    break;
            }
            udp_socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

