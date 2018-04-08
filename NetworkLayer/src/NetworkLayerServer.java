/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author CSE_BUET
 */
public class NetworkLayerServer {
    public static int clientCount = 0;
    public static ArrayList<Router> routers = new ArrayList<>();
    public static RouterStateChanger stateChanger = null;
    public static ArrayList<EndDevice> clientList=new ArrayList<>();
    /**
     * Each map entry represents number of client end devices connected to the interface
     */
    static Map<IPAddress,Integer> clientInterfaces = new HashMap<>();
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        /**
         * Task: Maintain an active client list
         */
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(1234);
        } catch (IOException ex) {
            Logger.getLogger(NetworkLayerServer.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.out.println("Server Ready: "+serverSocket.getInetAddress().getHostAddress());
        
        System.out.println("Creating router topology");
        
        readTopology();

        
        /**
         * Initialize routing tables for all routers
         */
        initRoutingTables();
        printRouters();
        
        /**
         * Update routing table using distance vector routing until convergence
         */
        DVR(1);
        //routers.get(0).updateRoutingTable(routers.get(4));
        //simpleDVR(1);
        printRouters();
        //routers.get(0).updateRoutingTable(routers.get(3));
        //printRouters();
        /**
         * Starts a new thread which turns on/off routers randomly depending on parameter Constants.LAMBDA
         */
        stateChanger = new RouterStateChanger();
        
        while(true){
            try {
                Socket clientSock = serverSocket.accept();
                System.out.println("Client attempted to connect");
                EndDevice host=getClientDeviceSetup();
                new ServerThread(clientSock,host);
            } catch (IOException ex) {
                Logger.getLogger(NetworkLayerServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        
    }
    
    public static void initRoutingTables()
    {
        for(int i=0;i<routers.size();i++)
        {
            routers.get(i).initiateRoutingTable();
        }
    }
    
    /**
     * Task: Implement Distance Vector Routing with Split Horizon and Forced Update
     */
    public static void DVR(int startingRouterId)
    {
        /**
         * pseudocode
         */

        boolean convergence=true;
        int p=0;
        if(!routers.get(startingRouterId-1).getState())
        {
            return;
        }
        while(convergence)
        {
            //convergence means no change in any routingTable before and after executing the following for loop
            convergence=false;
            for (int i = startingRouterId; i < Constants.INFTY; i++)
            {
                boolean x=false;
                Router r=routers.get(i-1);
                ArrayList<RoutingTableEntry> table=r.getRoutingTable();
                ArrayList<Integer> neighbours=r.getNeighborRouterIds();
                for (Integer n:neighbours)
                {
                    Router nR=routers.get(n-1);
                    boolean y=nR.updateRoutingTable(r);
                    x=x||y;
                    //nR.printRoutingTable();
                }
                convergence=convergence||x;
            }
            p++;
            System.out.println(p);
            //convergence=false;
            /*
            for each router r <starting from the router with routerId = startingRouterId, in any order>
            {
                1. T <- getRoutingTable of the router r
                2. N <- find routers which are the active neighbors of the current router r
                3. Update routingTable of each router t in N using the 
                   routing table of r [Hint: Use t.updateRoutingTable(r)]
            }*/
        }

    }
    
    /**
     * Task: Implement Distance Vector Routing without Split Horizon and Forced Update
     */
    public static void simpleDVR(int startingRouterId)
    {
        boolean convergence=true;
        int p=0;
        while(convergence)
        {
            //convergence means no change in any routingTable before and after executing the following for loop
            convergence = false;
            for (int i = startingRouterId; i < Constants.INFTY; i++) {
                boolean x = false;
                Router r = routers.get(i - 1);
                ArrayList<RoutingTableEntry> table = r.getRoutingTable();
                ArrayList<Integer> neighbours = r.getNeighborRouterIds();
                for (Integer n : neighbours) {
                    Router nR = routers.get(n - 1);
                    boolean y = nR.updateRoutingTableSimple(r);
                    x = x || y;
                    //nR.printRoutingTable();
                }
                convergence = convergence || x;
            }
            p++;
        }
        System.out.println(p);
    }
    
    
    public static EndDevice getClientDeviceSetup()
    {
        Random random = new Random();
        int r =Math.abs(random.nextInt(clientInterfaces.size()));
        
        System.out.println("Size: "+clientInterfaces.size()+"\n"+r);
        
        IPAddress ip=null;
        IPAddress gateway=null;
        
        int i=0;
        for (Map.Entry<IPAddress, Integer> entry : clientInterfaces.entrySet()) {
            IPAddress key = entry.getKey();
            Integer value = entry.getValue();
            if(i==r)
            {
                gateway = key;
                ip = new IPAddress(gateway.getBytes()[0]+"."+gateway.getBytes()[1]+"."+gateway.getBytes()[2]+"."+(value+2));
                value++;
                clientInterfaces.put(key, value);
                break;
            }
            i++;
        }
        
        EndDevice device = new EndDevice(ip, gateway);
        System.out.println("Device : "+ip+"::::"+gateway);
        return device;
    }
    
    public static void printRouters()
    {
        for(int i=0;i<routers.size();i++)
        {
            System.out.println("------------------\n"+routers.get(i));
            if(routers.get(i).getState())
                routers.get(i).printRoutingTable();
        }
    }
    
    public static void readTopology()
    {
        Scanner inputFile = null;
        try {
            inputFile = new Scanner(new File("topology.txt"));
            //skip first 27 lines
            int skipLines = 27;
            for(int i=0;i<skipLines;i++)
            {
                inputFile.nextLine();
            }
            
            //start reading contents
            while(inputFile.hasNext())
            {
                inputFile.nextLine();
                int routerId;
                ArrayList<Integer> neighborRouters = new ArrayList<>();
                ArrayList<IPAddress> interfaceAddrs = new ArrayList<>();
                
                routerId = inputFile.nextInt();
                
                int count = inputFile.nextInt();
                for(int i=0;i<count;i++)
                {
                    neighborRouters.add(inputFile.nextInt());
                }
                count = inputFile.nextInt();
                inputFile.nextLine();
                
                for(int i=0;i<count;i++)
                {
                    String s = inputFile.nextLine();
                    //System.out.println(s);
                    IPAddress ip = new IPAddress(s);
                    interfaceAddrs.add(ip);
                    
                    /**
                     * First interface is always client interface
                     */
                    if(i==0)
                    {
                        //client interface is not connected to any end device yet
                        clientInterfaces.put(ip, 0);
                    }
                }
                Router router = new Router(routerId, neighborRouters, interfaceAddrs);
                routers.add(router);
            }
            
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(NetworkLayerServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
