/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author samsung
 */
public class ServerThread implements Runnable {
    private Thread t;
    private Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    public EndDevice host;
    public Packet packet;
    public String path;
    public int hopCount;
    HashMap<Integer,ShowRoute> routes=new HashMap<Integer, ShowRoute>();

    public ServerThread(Socket socket,EndDevice host){
        
        this.socket = socket;
        try {
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(socket.getInputStream());
            
        } catch (IOException ex) {
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Server Ready for client "+NetworkLayerServer.clientCount);
        this.host=host;
        NetworkLayerServer.clientCount++;
        NetworkLayerServer.clientList.add(host);
        t=new Thread(this);
        t.start();
    }

    @Override
    public void run() {
        /**
         * Synchronize actions with client.
         */
            try {
                int dropCount=0;
                int totalHop=0;
                output.writeObject(host);
                System.out.println("Client Count is "+NetworkLayerServer.clientCount+" for host "+host.getIp());
                //while (NetworkLayerServer.clientCount < 3) ;
                //System.out.println("Client Count is "+NetworkLayerServer.clientCount+" for host "+host.getIp());
                output.writeObject(NetworkLayerServer.clientCount);
                for (int j = 0; j <100 ; j++)
                {
                    packet = (Packet) input.readObject();
                    //int rand = (int) (Math.random() % (NetworkLayerServer.clientList.size()));
                    Random x = new Random();
                    int rand=x.nextInt(10000000);
                    rand=rand%(NetworkLayerServer.clientList.size());
                    EndDevice receiever = NetworkLayerServer.clientList.get(rand);
                    packet.setDestinationIP(receiever.getIp());
                    packet.setDestinationNetwork(receiever.getIp());

                    System.out.println("Packet Receieved");

                    //String returnMsg = deliverPacket(packet);

                    if (packet.getSpecialMessage().equals("")) {
                        if (!deliverPacket(packet)) {
                            output.writeObject(path);
                            dropCount++;
                        } else
                        {
                            for (EndDevice e :NetworkLayerServer.clientList)
                            {
                                if(e.getIp()==receiever.getIp())
                                {
                                    System.out.println("Packet sent to client::"+receiever.getIp());
                                    break;
                                }
                            }
                            output.writeObject("Hopcount is "+hopCount+" "+path);
                            totalHop=totalHop+hopCount;
                        }
                    } else {
                        for (Router r : NetworkLayerServer.routers) {
                            packet.setDestinationNetwork(r.getInterfaceAddrs().get(0));
                            //String msg = deliverPacket(packet);
                            //output.writeObject(r);
                            if (!deliverPacket(packet)) {
                                routes.put(r.getRouterId(), new ShowRoute(hopCount, "Packet Dropped,you can watch last trace " + path));
                                //dropCount++;
                            } else {
                                routes.put(r.getRouterId(), new ShowRoute(hopCount, path));
                                totalHop=totalHop+hopCount;
                            }
                        }
                        output.writeObject(routes);
                        output.writeObject(NetworkLayerServer.routers);
                    }
                }
                output.writeObject(dropCount);
                double averageHopcount=((double)totalHop/(100-dropCount));
                output.writeObject(averageHopcount);
                System.out.println("Drop count is: "+dropCount);
                System.out.println("Average hopcount is: "+(double)totalHop/(100-dropCount));
            } catch (Exception e) {
                e.printStackTrace();
            }

            //Tasks:
            //1. Upon receiving a packet server will assign a recipient.
            //int rand=(int) (Math.random()%NetworkLayerServer.clientList.size());
        /*[Also modify packet to add destination]
        2. call deliverPacket(packet)
        */

        /*
        3. If the packet contains "SHOW_ROUTE" request, then fetch the required information
                and send back to client
        4. Either send acknowledgement with number of hops or send failure message back to client
        */
    }
    
    /**
     * Returns true if successfully delivered
     * Returns false if packet is dropped
     * @param p
     * @return 
     */
    public boolean deliverPacket(Packet p)
    {

        //1. Find the router s which has an interfacec
        //        such that the interface and source end device have same network address.
        Router src = null;
        Router dest = null;
        Router next = null;
        path="";
        hopCount=0;
        for (Router r:NetworkLayerServer.routers)
        {
            if(p.getSourceNetwork().toString().equals(r.getInterfaceAddrs().get(0).toString()))
            {
                src=r;
                break;
            }

        }
        path=path+"Source: "+src.getRouterId()+" ; ";
        System.out.println("Source is "+src.getRouterId());
        //2. Find the router d which has an interface
        //        such that the interface and destination end device have same network address.
        for (Router r:NetworkLayerServer.routers)
        {
            if(p.getDestinationNetwork().toString().equals(r.getInterfaceAddrs().get(0).toString()))
            {
                dest=r;
                break;
            }

        }
        path=path+"Destination: "+dest.getRouterId()+" ; ";
        System.out.println("Destinition is "+dest.getRouterId());
        if(!src.getState())
        {
            System.out.println("Source switched off");
            path=path+"[Dropped]";
            return false;
        }
        ArrayList<RoutingTableEntry> routingTable=src.getRoutingTable();
        double nextDistance=routingTable.get(dest.getRouterId()-1).getDistance();
        if(nextDistance<Constants.INFTY&&routingTable.size()>0)
        {
            next = NetworkLayerServer.routers.get(routingTable.get(dest.getRouterId() - 1).getGatewayRouterId() - 1);
        }
        else
        {
            System.out.println("Packet Droppped destination down");
            path=path+src.getRouterId()+"[Dropped]";
            return false;
        }
        path=path+src.getRouterId()+">";
        while (next.getRouterId()!=dest.getRouterId())
        {
            if(next.getState()==false)
            {
                System.out.println("Packet Dropped");
                path=path+"[Dropped]";
                routingTable.get(dest.getRouterId()-1).setDistance(Constants.INFTY);
                routingTable.get(dest.getRouterId()-1).setGatewayRouterId(0);
                //synchronized (NetworkLayerServer.stateChanger) {
                    try {
                        NetworkLayerServer.stateChanger.t.stop();
                        if(src.getState()) {
                            NetworkLayerServer.DVR(src.getRouterId());
                            //NetworkLayerServer.simpleDVR(src.getRouterId());
                        }
                        NetworkLayerServer.stateChanger=new RouterStateChanger();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                //}
                return false;

            }
            path=path+next.getRouterId()+">";
            ArrayList<RoutingTableEntry> nextTable=next.getRoutingTable();
            if(nextTable.get(src.getRouterId()-1).getDistance()>=Constants.INFTY)
            {
                nextTable.get(src.getRouterId()-1).setDistance(1);
                nextTable.get(src.getRouterId()-1).setGatewayRouterId(src.getRouterId());
                //synchronized (NetworkLayerServer.stateChanger) {
                    try {
                        NetworkLayerServer.stateChanger.t.stop();
                        if(next.getState())
                        {
                            NetworkLayerServer.DVR(next.getRouterId());
                            //NetworkLayerServer.simpleDVR(src.getRouterId());
                        }
                        NetworkLayerServer.stateChanger=new RouterStateChanger();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                //}
            }
            src=next;
            ArrayList<RoutingTableEntry> nextRoutingTable=next.getRoutingTable();
            double nd=nextRoutingTable.get(dest.getRouterId()-1).getDistance();
            if(nd<Constants.INFTY&&nextRoutingTable.size()>0)
            {
                next = NetworkLayerServer.routers.get(nextRoutingTable.get(dest.getRouterId() - 1).getGatewayRouterId() - 1);
            }
            else
            {
                System.out.println("Packet Droppped");
                path=path+"[Dropped]";
                return false;
            }
            //next=NetworkLayerServer.routers.get(next.getRoutingTable().get(dest.getRouterId()-1).getGatewayRouterId()-1);
            hopCount++;
        }
        path=path+next.getRouterId();
        hopCount++;
        System.out.println(path);
        return true;
        /*
        3. Implement forwarding, i.e., s forwards to its gateway router x considering d as the destination.
                similarly, x forwards to the next gateway router y considering d as the destination, 
                and eventually the packet reaches to destination router d.
                
            3(a) If, while forwarding, any gateway x, found from routingTable of router x is in down state[x.state==FALSE]
                    (i) Drop packet
                    (ii) Update the entry with distance Constants.INFTY
                    (iii) Block NetworkLayerServer.stateChanger.t
                    (iv) Apply DVR starting from router r.
                    (v) Resume NetworkLayerServer.stateChanger.t
                            
            3(b) If, while forwarding, a router x receives the packet from router y, 
                    but routingTableEntry shows Constants.INFTY distance from x to y,
                    (i) Update the entry with distance 1
                    (ii) Block NetworkLayerServer.stateChanger.t
                    (iii) Apply DVR starting from router x.
                    (iv) Resume NetworkLayerServer.stateChanger.t
                            
        4. If 3(a) occurs at any stage, packet will be dropped, 
            otherwise successfully sent to the destination router
        */
    }



    @Override
    public boolean equals(Object obj) {
        return super.equals(obj); //To change body of generated methods, choose Tools | Templates.
    }

}
