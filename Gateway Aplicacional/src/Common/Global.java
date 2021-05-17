package Common;

public class Global {


    public static String byteArraytoHexString(byte[] message){
        StringBuilder sb = new StringBuilder();

        for(byte b : message){
            sb.append(String.format("%02x " , b));
        }

        return sb.toString();
    }
}
