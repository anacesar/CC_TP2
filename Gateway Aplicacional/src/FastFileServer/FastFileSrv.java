package FastFileServer;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import Common.FSChunkProtocol;
import PDU.PDU;

public class FastFileSrv {
    private int port;
    private InetAddress server_address;
    private int my_port;
    private InetAddress ffs_address;
    private String files_path;
    private DatagramSocket udp_socket;
    private boolean shutdown = false;

    public FastFileSrv(int port, InetAddress sAddress, String files_path) throws SocketException{
        this.port = port;
        this.server_address = sAddress;
        this.files_path = System.getProperty("user.dir")+ "/src/" + files_path;
        this.my_port = new Random().nextInt((2000 - 1000) + 1) + 1000;
        this.udp_socket = new DatagramSocket(my_port);
        this.ffs_address = this.udp_socket.getLocalAddress();
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
            //FileInputStream is = new FileInputStream(new File(files_path + "/" + filename));
            RandomAccessFile raf = new RandomAccessFile(files_path + "/" + filename , "rw");

            raf.seek(offset);
            raf.read(data, 0, size);
            //System.out.println("PDU : " + pdu.getType() + " : " + new String(data));
            /*is.skipNBytes(offset);
            is.read(data, 0, size);
            is.close();*/
        } catch(IOException e) {
            return null;
        }

        System.out.println("data length after file writing " + data.length);
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
                    PDU pdu = PDU.fromBytes(message, receive.getLength());

                    System.out.println("ffs received : " + pdu.toString());
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
                                default:
                                    pdu_answer = pdu;
                            }
                            break;
                        case 1: /* data pdu */
                            switch(pdu.getType()){
                                case 0: /* file request */
                                    System.out.println("received file request " + new String(pdu.getData()));
                                    /* check for file in directory */
                                    long length = find(new String(pdu.getData()));
                                    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
                                    buffer.putLong(length);
                                    pdu_answer = new PDU(1, 1, pdu.getSeq_number(), buffer.array());
                                    System.out.println("sending pdu with file size " + length + " with seq_number " + pdu_answer.getSeq_number());
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

            System.out.println("sended " + pdu.toString());
        }catch(IOException e) {
            e.printStackTrace();
        }

    }



}
