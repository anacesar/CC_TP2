package Gateway;

import java.io.OutputStream;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
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
    private int i = 0;

    public HttpGW(){
        this.http_responses = new HashMap<>();
        this.fastfileservers = new ConcurrentHashMap<>();
        this.pdus = new HashMap<>();
        this.lock = new ReentrantLock();
        this.protocolCondition = new HashMap<>();
        this.nr_request = 1;

        new Thread(() ->  {
            try {
                while(true) {
                    Thread.sleep(2000);
                    System.out.println("ver se os ffservers muuuuleram");
                    /* check if all ffservers are still active */
                    fastfileservers.forEach((port, address) -> {
                        System.out.println("trying to connect to ffserver in port " + port);
                        FSChunkProtocol protocol = new FSChunkProtocol(new PDU(0, 6, nr_request++), port, address);
                        if(protocol.send() == 1) {
                            System.out.println("timeout due to inactivity of ffserver in port " + port);
                            fastfileservers.remove(port);
                            protocolCondition.values().stream().forEach(p -> p.ffsDown(port));
                        } else System.out.println("ffserver in port " + port + " is still active");
                    });
                }
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void controlPDU(PDU pdu){
        switch (pdu.getType()){
            case 0: /* HELLO */
                new Thread(new FSChunkProtocol(new PDU(0, 1), pdu.getPort(), pdu.getInetAddress())).start();
                break;
            case 2: /* PASS */
                /* check for password */
                PDU pdu_answer;
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
            case 7: /* fast file server active */
                dataPDU(pdu);
                break;
        }
    }

    private void http_response(int seq_number, String filename) {
        //System.out.println("trying to get pdus with seq_number " + seq_number);
        //File file = new File(filename);
        System.out.println("size before sorted arraylist " + pdus.get(seq_number).size());

        pdus.get(seq_number).sort(Comparator.comparingInt(o -> o.getType()));

        System.out.println("size of sorted arraylist " + pdus.get(seq_number).size());
        OutputStream out = this.http_responses.get(seq_number);
        long total_length = pdus.get(seq_number).stream().mapToLong(PDU::getDataSize).sum();
        String headers = "HTTP/1.1 200 OK\nDate: " + LocalDateTime.now() ;
        try {

            //OutputStream os = new FileOutputStream(file);
            for(PDU pdu : pdus.get(seq_number)) {
                //os.write(pdu.getData());
                out.write(pdu.getData());
            }
            //out.write(headers.getBytes());

            //System.out.println("PDU : " + pdu.getType() + " : " + new String(pdu.getData()));
            //System.out.println("Write "+ pdu.getData().length +" bytes to file.");
            //os.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public void dataPDU(PDU pdu){
        /* pdu received about a transfer */
        /* type is sequence number in this case */
        FSChunkProtocol protocol = protocolCondition.get(pdu.getSeq_number());
        if(protocol == null){  // create a new request
            //System.out.println("SOMETHING IS WROOOOOONG !!!!!!!");
            protocol = new FSChunkProtocol(pdu, pdu.getPort(), pdu.getInetAddress());
            protocolCondition.put(nr_request, protocol);
            pdus.put(nr_request, new ArrayList<>());
            new Thread(protocol).start();
        }else{
            System.out.println("pdu "  + this.i + "received");
            if(pdu.getType() > 1){ pdus.get(pdu.getSeq_number()).add(pdu); this.i++;}
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