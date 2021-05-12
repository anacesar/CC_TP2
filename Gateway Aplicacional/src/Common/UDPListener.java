package Common;

import Gateway.HttpGW;

public class UDPListener implements Runnable{
    private HttpGW gateway;
    private int my_port;


    public UDPListener(HttpGW gateway, int my_port){
        this.gateway = gateway;
        this.my_port = my_port;
    }


    public void run(){
       
        
    }

    
}