package FastFileServer;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

import Common.FSChunkProtocol;
import Common.Global;
import PDU.PDU;

public class FastFileSrv {
    private int port;
    private InetAddress server_address;
    private int my_port;
    private String files_path;
    private DatagramSocket udp_socket;
    private boolean shutdown = false;

    private Lock lock = new ReentrantLock();

    public FastFileSrv(int port, InetAddress sAddress, String files_path) throws SocketException{
        this.port = port;
        this.server_address = sAddress;
        //this.files_path = System.getProperty("user.dir")+ "/src/" + files_path;
        this.files_path = Global.makePath(System.getProperty("user.dir") , files_path);
        this.my_port = new Random().nextInt((2000 - 1000) + 1) + 1000;
        this.udp_socket = new DatagramSocket(my_port);
    }

    public long find(String fileName) {
        AtomicLong result = new AtomicLong();
        try(Stream<Path> paths = Files.walk(Paths.get(files_path))){
            paths.forEach(path -> {
                System.out.println("file path " + path.getFileName().toString());
                if(path.getFileName().toString().equals(fileName)) {
                    try {
                        result.set(Files.size(path));
                    } catch(IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }catch(IOException e){e.printStackTrace();}

        return result.get();

    }

    public PDU getChunk(PDU pdu){
        int offset = ByteBuffer.wrap(Arrays.copyOfRange(pdu.getData(), 0, 4)).getInt();
        int size = (int)ByteBuffer.wrap(Arrays.copyOfRange(pdu.getData(), 4, 12)).getLong();
        int length = ByteBuffer.wrap(Arrays.copyOfRange(pdu.getData(), 12, 16)).getInt();
        byte[] filebyte = ByteBuffer.wrap(Arrays.copyOfRange(pdu.getData(), 16, 16 + length)).array();
        String filename = new String(filebyte);
        System.out.println("offset : " + offset + " size : " + size + " filename " + filename);
        byte[] data = new byte[size];

        try {
            RandomAccessFile raf = new RandomAccessFile(files_path + "/" + filename , "rw");

            raf.seek(offset);
            raf.read(data, 0, size);

        } catch(IOException e) {
            return null;
        }

        System.out.println("data length after file writing " + data.length);
        System.out.println("sendind pdu with type : " + pdu.getType() + " and seq_nr " + pdu.getSeq_number());
        return new PDU(1, pdu.getType(), pdu.getSeq_number(), data);
    }

    public void run() throws InterruptedException {
        /* listening thread */
        new Thread(() -> {
            try{
                while(!shutdown){
                    /* Receive a packet from gateway(via protocol) */
                    byte[] message = new byte[2000];
                    DatagramPacket receive = new DatagramPacket(message, message.length);
                    udp_socket.receive(receive);
                    this.lock.lock();
                    PDU pdu = PDU.fromBytes(message, receive.getLength());

                    System.out.println("ffs received " + pdu.toString());

                    new Thread(() -> {
                        /* timeout for communication with gateway */
                        //while(this.queue.isEmpty()) this.queue.wait(1000);
                        PDU pdu_answer = null;

                        switch(pdu.getFlag()){
                            case 0 : /* control pdu */
                                switch(pdu.getType()){
                                    case 1: /* receive CHECK*/
                                        System.out.println("Bro da me a pass");
                                        String pass = new Scanner(System.in).nextLine();
                                        pdu_answer = new PDU(0,2, 0, pass.getBytes());
                                        break;
                                    case 3: /* receive ACTIVE */
                                        if(new String(pdu.getData()).equals("YES"))
                                            System.out.println("You are know able to connect to hhtpGW with ip " + server_address.getHostAddress() + " and port " + port);
                                        else{
                                            System.out.println("The password is not valid! Closing connection...");
                                            shutdown = true;
                                        }
                                        break;
                                    case 6: /* gw is asking if im active*/
                                        System.out.println("responding to active ?? ");
                                        pdu_answer = new PDU(0,7,pdu.getSeq_number());
                                        break;
                                    default:
                                        pdu_answer = pdu;
                                        break;
                                }
                                break;
                            case 1: /* data pdu */
                                switch(pdu.getType()){
                                    case 0: /* file request */
                                        /* check for file in directory */
                                        long length = find(new String(pdu.getData()));
                                        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
                                        buffer.putLong(length);
                                        pdu_answer = new PDU(1, 1, pdu.getSeq_number(), buffer.array());
                                        break;
                                    default: /* chunk request */
                                        pdu_answer = getChunk(pdu);
                                        break;
                                }
                                break;
                        }

                        /* ffs send response */
                        if(pdu_answer != null){
                            new FSChunkProtocol(pdu_answer, port, server_address).send(this.udp_socket);
                        }
                    }).start();
                    this.lock.unlock();
                }
                udp_socket.close();
            }catch(IOException  e){e.printStackTrace();}
        }).start();

        /* ask for authentication */
        try{
            PDU pdu = new PDU(0,0);

            byte[] message = pdu.toBytes();
            DatagramPacket packet_send = new DatagramPacket(message, message.length, server_address, port);
            udp_socket.send(packet_send);

        }catch(IOException e) {
            e.printStackTrace();
        }

    }



}
