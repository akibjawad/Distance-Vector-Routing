/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.Serializable;

/**
 *
 * @author samsung
 */
public class Packet implements Serializable {
    private String message;
    private String specialMessage;  //ex: "SHOW_ROUTE" request
    private IPAddress destinationIP;
    private IPAddress sourceIP;
    public IPAddress sourceNetwork;
    public IPAddress destinationNetwork;

    public Packet(String message, String specialMessage, IPAddress sourceIP, IPAddress destinationIP) {
        this.message = message;
        this.specialMessage = specialMessage;
        this.sourceIP = sourceIP;
        this.destinationIP = destinationIP;

    }

    public IPAddress getSourceNetwork() { return sourceNetwork; }

    public IPAddress getDestinationNetwork() { return destinationNetwork; }

    public void setSourceNetwork(IPAddress sourceIP)
    {
        Short[] s=sourceIP.getBytes();
        s[3]=1;
        this.sourceNetwork=new IPAddress(s);
    }

    public void setDestinationNetwork(IPAddress destinationIP)
    {
        Short[] d=destinationIP.getBytes();
        d[3]=1;
        this.destinationNetwork=new IPAddress(d);
    }

    public IPAddress getSourceIP() {
        return sourceIP;
    }

    public void setSourceIP(IPAddress sourceIP) {
        this.sourceIP = sourceIP;
    }
    
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSpecialMessage() {
        return specialMessage;
    }

    public void setSpecialMessage(String specialMessage) {
        this.specialMessage = specialMessage;
    }

    public IPAddress getDestinationIP() {
        return destinationIP;
    }

    public void setDestinationIP(IPAddress destinationIP) {
        this.destinationIP = destinationIP;
    }
    
    
    
}
