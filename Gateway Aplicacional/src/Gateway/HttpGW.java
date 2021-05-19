package Gateway;

import java.io.OutputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import Common.FSChunkProtocol;
import PDU.PDU;

public class HttpGW{
    
    /* mapa cliente-connection */
    /* mapa cliente-list<pedidos> */
    /* pool de ffs */
    //Map<FastFileServer,SocketAddress> fastfileservers;
    //Map<InterAddress,FastFileServer> fastfileservers;
    /* id_request  */
    private ConcurrentHashMap<Integer, List<byte[]>> pdus;
    private Lock lock;
    private HashMap<Integer, FSChunkProtocol> protocolCondition;
    private int nr_request;

    public HttpGW(){
        this.pdus = new ConcurrentHashMap<>();
        this.lock = new ReentrantLock();
        this.protocolCondition = new HashMap<>();
        this.nr_request = 0;

    }

    public void controlPDU(PDU pdu){
        switch (pdu.getType()){
            case 00: /* HELLO */
                System.out.println("GW received fasfileserver HELLO");
                new Thread(new FSChunkProtocol(new PDU(0, 01), pdu.getPort(), pdu.getInetAddress())).start();
        }
    }


    public void receive(PDU pdu){

        FSChunkProtocol protocol = protocolCondition.get(nr_request);

        /* check for flag */
        switch(pdu.getFlag()){
            case 0:
                controlPDU(pdu);
                break;
            case 1:
                //dataPDU();
                break;
        }

        /*
        if(protocol == null){  // create a new request
            protocol = new FSChunkProtocol(pdu, pdu.getPort(), pdu.getInetAddress());
            protocolCondition.put(nr_request, protocol);
            new Thread(protocol).start();
        }else{
            protocol.pdu(pdu);
        }

        */

        /* create protocol */

    }


    public void file_request(String filename, OutputStream out){
        /* create pdu with nr_request to ask for filename in broadcast */

    }
    
}