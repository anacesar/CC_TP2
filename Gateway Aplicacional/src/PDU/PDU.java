package PDU;

import Enum.TypeEnum;

public abstract class PDU implements Comparable<PDU>{
    private long checksum;
    private int flag;
    private TypeEnum type;


    public PDU(long checksum, int flag, TypeEnum type) {
        this.checksum = checksum;
        this.flag = flag;
        this.type = type;
    }


    public long getChecksum(){
        return this.checksum;
    }

    public void setChecksum(long c){
        this.checksum = c;
    }

    public int getFlag(){
        return this.flag;
    }

    public void setFlag(int flag){
        this.flag = flag;
    }

    public TypeEnum getType(){
        return this.type;
    }

    public void setType(TypeEnum t){
        this.type = t;
    }

    public abstract void fromBytes(byte[] pdu, int length) ;
    @Override
    public String toString() {
        return "PDU {" +
                "checksum= " + checksum +
                " , flag= " + flag +
                " , type= " + type +
                '}';
    }


    public int compareTo(DataPDU o){
        return this.getSeq_number() - o.getSeq_number();
    }

}
