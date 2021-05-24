package PDU;

import Enum.TypeEnum;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Comparator;

public class PDU{
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
        this.seq_number = 0;
        this.data = new byte[1448];
    }

    public PDU(int flag, int type, int seq_number) {
        //this.checksum = checksum;
        this.flag = flag;
        this.type = type;
        this.seq_number = seq_number;
        this.data = new byte[1448];
    }

    public PDU(int flag, int type, int seq_number, byte[] data) {
        //this.checksum = checksum;
        this.flag = flag;
        this.type = type;
        this.seq_number = seq_number;
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

    public byte[] getData(){
        return this.data;
    }

    public long getDataSize(){ return this.data.length; }

    public void setData(byte[] data){
        this.data = data.clone();
    }

    public InetAddress getInetAddress(){ return this.address;}

    public void setInetAddress(InetAddress address){this.address = address;}

    public int getPort(){ return this.port;}

    public void setPort(int port){this.port = port;}

    /* Obtains a PDU from a byte array that another server sent */
     public static PDU fromBytes(byte[] pdu, int length) {
        int flag = ByteBuffer.wrap(Arrays.copyOfRange(pdu, 0, 4)).getInt();
        int type = ByteBuffer.wrap(Arrays.copyOfRange(pdu, 4, 8)).getInt();
        int seq_number = ByteBuffer.wrap(Arrays.copyOfRange(pdu, 8, 12)).getInt();
        byte[] data = ByteBuffer.wrap(Arrays.copyOfRange(pdu, 12, length)).array();

        return new PDU(flag,type,seq_number,data);
    }

    /* Transforms the PDU into a byte array ready to be sent*/
    public byte[] toBytes(){
        ByteBuffer packet = ByteBuffer.allocate(12 + this.data.length);

        //packet.put(ByteBuffer.allocate(8).putLong(getChecksum()).array());
        packet.put(ByteBuffer.allocate(4).putInt(this.flag).array());
        packet.put(ByteBuffer.allocate(4).putInt(this.type).array());
        packet.put(ByteBuffer.allocate(4).putInt(this.seq_number).array());

        packet.put(this.data);

        return packet.array();
    }


    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("| PDU | ");
        sb.append("flag = " + flag + " type = " + type + " seq_number " + seq_number + " data = " + data);

        return sb.toString();
    }


}
