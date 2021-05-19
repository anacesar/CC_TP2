package PDU;


import Enum.TypeEnum;

import java.nio.ByteBuffer;
import java.util.Arrays;


public class DataPDU extends PDU {
    private int seq_number;
    private byte[] data; /* corresponde aos dados da mensagem  */

    public DataPDU(long checksum, int flag, TypeEnum type, int seq_number, byte[] data){
        super(checksum,flag,type);
        this.seq_number = seq_number;
        this.data = data;
    }

    /* Transforms the PDU into a byte array ready to be sent*/
    public byte[] toBytes(){
        ByteBuffer packet = ByteBuffer.allocate(20 + this.data.length);

        packet.put(ByteBuffer.allocate(8).putLong(getChecksum()).array());
        packet.put(ByteBuffer.allocate(4).putInt(getFlag()).array());

        /* TODO test to see if enum works here */
        packet.put(ByteBuffer.allocate(4).putInt(getType().ordinal()).array());

        packet.put(ByteBuffer.allocate(4).putInt(this.seq_number).array());

        packet.put(this.data);

        return packet.array();
    }


    /* Obtains a PDU from a byte array that another server sent */
    public void fromBytes(byte[] pdu, int length) {
        seq_number = ByteBuffer.wrap(Arrays.copyOfRange(pdu, 0, 4)).getInt();
        data = ByteBuffer.wrap(Arrays.copyOfRange(pdu, 20, length)).array();

    }

    public int getSeq_number(){
        return this.seq_number;
    }


}
