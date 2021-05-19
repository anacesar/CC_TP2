package PDU;

import Enum.TypeEnum;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Comparator;

public class PDU implements Comparable<PDU> {
    //private long checksum;
    private int flag; /* 0 - Controlo & 1 - Dados */
    private int type; 
    private int seq_number;
    private byte[] data; /* corresponde aos dados da mensagem  */

    private InetAddress address;
    private int port;

    public PDU(int flag, int type) {
        //this.checksum = checksum;
        this.flag = flag;
        this.type = type;
        this.data = new byte[1448];
    }
    
    public PDU(int flag, int type, byte[] data) {
        //this.checksum = checksum;
        this.flag = flag;
        this.type = type;
        this.data = data.clone();
    }

    /*
    public long getChecksum(){
        return this.checksum;
    }

    public void setChecksum(long c){
        this.checksum = c;
    }
    */

    public int getFlag(){
        return this.flag;
    }

    public void setFlag(int flag){
        this.flag = flag;
    }

    public int getType(){
        return this.type;
    }

    public void setType(int t){
        this.type = t;
    }

    public int getSeq_number(){
        return this.seq_number;
    }

    public InetAddress getInetAddress(){ return this.address;}

    public void setInetAddress(InetAddress address){this.address = address;}

    public int getPort(){ return this.port;}

    public void setPort(int port){this.port = port;}

    /* Obtains a PDU from a byte array that another server sent */
     public static PDU fromBytes(byte[] pdu, int length) {
        int flag = ByteBuffer.wrap(Arrays.copyOfRange(pdu, 0, 4)).getInt();
        int type = ByteBuffer.wrap(Arrays.copyOfRange(pdu, 4, 8)).getInt();
         //seq_number = ByteBuffer.wrap(Arrays.copyOfRange(pdu, 0, 4)).getInt();
        byte[] data = ByteBuffer.wrap(Arrays.copyOfRange(pdu, 8, length)).array();

        return new PDU(flag,type,data);
    }

    /* Transforms the PDU into a byte array ready to be sent*/
    public byte[] toBytes(){
        ByteBuffer packet = ByteBuffer.allocate(8 + this.data.length);

        //packet.put(ByteBuffer.allocate(8).putLong(getChecksum()).array());
        packet.put(ByteBuffer.allocate(4).putInt(getFlag()).array());
        packet.put(ByteBuffer.allocate(4).putInt(getType()).array());
        //packet.put(ByteBuffer.allocate(4).putInt(this.seq_number).array());

        packet.put(this.data);

        return packet.array();
    }

    public int compareTo(PDU o){
        return this.getSeq_number() - o.getSeq_number();
    }
    
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(": PDU : \n");
        sb.append("flag = " + flag + " type = " + type + " data = " + data);

        return sb.toString();
    }


}
