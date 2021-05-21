package Gateway;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.ParameterizedType;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import Common.FSChunkProtocol;
import PDU.PDU;

public class HttpGW{
    private final static String password = "protocol";

    /* mapa cliente-connection */
    private Map<Integer, OutputStream> http_responses;
    /* pool de ffs */
    //private Map<InetAddress, Integer> fastfileservers;
    private Map<Integer, InetAddress> fastfileservers;

    /* id_request  */
    private Map<Integer, List<PDU>> pdus;
    private Lock lock;
    private HashMap<Integer, FSChunkProtocol> protocolCondition;
    private int nr_request;

    public HttpGW(){
        this.http_responses = new HashMap<>();
        this.fastfileservers = new HashMap<>();
        this.pdus = new HashMap<>();
        this.lock = new ReentrantLock();
        this.protocolCondition = new HashMap<>();
        this.nr_request = 1;

    }

    public void controlPDU(PDU pdu){
        switch (pdu.getType()){
            case 0: /* HELLO */
                System.out.println("GW received fasfileserver HELLO");
                new Thread(new FSChunkProtocol(new PDU(0, 01), pdu.getPort(), pdu.getInetAddress())).start();
                break;
            case 2: /* PASS */
                System.out.println("GW received fasfileserver PASS");
                /* check for password */
                PDU pdu_answer;
                System.out.println("password atempt : "+ new String(pdu.getData()));
                if(password.equals(new String(pdu.getData()))){
                    //fastfileservers.put(pdu.getInetAddress(), pdu.getPort());
                    fastfileservers.put(pdu.getPort(), pdu.getInetAddress());
                    System.out.println("added new fastfile server : PORT " + pdu.getPort() + " ADDRESS " + pdu.getInetAddress() );
                    pdu_answer = new PDU(0, 3, nr_request++,"YES".getBytes());
                }else
                    pdu_answer = new PDU(0, 3,nr_request++, "NO".getBytes());

                new Thread(new FSChunkProtocol(pdu_answer, pdu.getPort(), pdu.getInetAddress())).start();
                break;
            case 5:  /* end of file transfer */
                System.out.println("end of file detected");
                http_response(pdu.getSeq_number(), new String(pdu.getData()));
                break;
        }
    }

    private void http_response(int seq_number, String filename) {
        System.out.println("trying to get pdus with seq_number " + seq_number);
        File file = new File(filename);

        for(PDU pdu : pdus.get(seq_number)){
            try {
                OutputStream os = new FileOutputStream(file);
                os.write(pdu.getData());
                System.out.println("Write "+ pdu.getData().length +" bytes to file.");
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public void dataPDU(PDU pdu){
        /* pdu received about a transfer */
        /* type is sequence number in this case */
        FSChunkProtocol protocol = protocolCondition.get(pdu.getSeq_number());
        if(protocol == null){  // create a new request
            System.out.println("SOMETHING IS WROOOOOONG !!!!!!!");
            protocol = new FSChunkProtocol(pdu, pdu.getPort(), pdu.getInetAddress());
            protocolCondition.put(nr_request, protocol);
            pdus.put(nr_request, new ArrayList<>());
            new Thread(protocol).start();
        }else{
            System.out.println("new pdu received for nr_request " + pdu.getSeq_number());
            if(pdu.getType() > 1) pdus.get(pdu.getSeq_number()).add(pdu);
            protocol.pdu(pdu);
        }
    }

    public void receive(PDU pdu){

        /* check for flag */
        switch(pdu.getFlag()){
            case 0:
                controlPDU(pdu);
                break;
            case 1:
                dataPDU(pdu);
                break;
        }
    }


    public void file_request(String filename, OutputStream out){
        int my_request = nr_request++;
        /* create pdu with nr_request to ask for filename in broadcast */
        PDU broadcast_pdu = new PDU(1, 0, my_request, filename.getBytes());
        System.out.println("broadcast pdu seq_number " + broadcast_pdu.getSeq_number());

        /* create protocol to deal with file request */
        FSChunkProtocol file_protocol = new FSChunkProtocol(broadcast_pdu, new HashMap<>(this.fastfileservers));
        protocolCondition.put(my_request, file_protocol);

        /* associate nr_request with clients outputstream */
        this.http_responses.put(my_request, out);
        this.pdus.put(my_request, new ArrayList<>());

        new Thread(file_protocol).start();
    }
    
}