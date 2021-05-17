package Gateway;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import Common.FSChunkProtocol;

public class HttpGW{
    
    /* mapa cliente-connection */
    /* mapa cliente-list<pedidos> */
    /* pool de ffs */
    //Map<FastFileServer,InetAddress> fastfileservers;
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


    public void receive(DatagramPacket packet){
        byte[] message = packet.getData();
        //System.out.println("httpgw : " + new String(message));
        
        int nr_request = ByteBuffer.wrap(Arrays.copyOfRange(message, 0, 4)).getInt();
        int port = ByteBuffer.wrap(Arrays.copyOfRange(message, 4, 8)).getInt();

        System.out.println("HTTPGW !! nr_request " + nr_request + " and port " + port);
        
        FSChunkProtocol protocol = protocolCondition.get(nr_request);

        if(protocol == null){  /* create a new request */
            protocol = new FSChunkProtocol(message, packet.getPort(), packet.getAddress(), packet.getAddress());
            protocolCondition.put(nr_request, protocol);
            new Thread(protocol).start();
        }else{
            protocol.pdu(message);
        }

        /* create protocol */
        

    }
    
}