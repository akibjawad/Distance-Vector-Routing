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
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author samsung
 */
public class Client {
    public static void main(String[] args)
    {
        Socket socket;
        ObjectInputStream input = null;
        ObjectOutputStream output = null;
        EndDevice own = null;
        
        try {
            socket = new Socket("localhost", 1234);
            input = new ObjectInputStream(socket.getInputStream());
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        System.out.println("Connected to server");
        /**
         * Tasks
         */
        /*
        1. Receive EndDevice configuration from server
        */
        try
        {
            own = (EndDevice) input.readObject();
            System.out.println("IP::"+own.getIp()+" Gateway::"+own.getGateway());
            System.out.println("If you want to start press y");
            Scanner scanner=new Scanner(System.in);
            String oops=scanner.nextLine();
            if(oops.equals("y"))
            {
                int clientcount=(int)input.readObject();
                System.out.println("Client now "+ clientcount +" starting");
            }
            else
            {
                return;
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        //ArrayList<EndDevice> clientList=NetworkLayerServer.clientList;
        //while (NetworkLayerServer.clientList.size()<3);
        /*
        2. [Adjustment in NetworkLayerServer.java: Server internally
            handles a list of active clients.]
        */
        for(int i=0;i<100;i++)
        {
              //Generate a random message
            String message="Hello"+i;
            //[Adjustment in ServerThread.java] Assign a random receiver from active client list
            //int rand=(int) (Math.random()%(NetworkLayerServer.clientList.size()));
            //EndDevice receiever=(NetworkLayerServer.clientList.get(rand));
            Packet packet;
            if(i==20)
            {
                //continue;
                //Send the messageto server and a special request "SHOW_ROUTE"
                packet=new Packet("","SHOW_ROUTE",own.getIp(),null);
                packet.setSourceNetwork(own.getIp());
                try
                {
                    output.writeObject(packet);
                    HashMap<Integer,ShowRoute> routes = (HashMap<Integer, ShowRoute>) input.readObject();
                    ArrayList<Router> router= (ArrayList<Router>) input.readObject();
                    for (Router r: router)
                    {
                        System.out.println("Router ID: "+r.getRouterId());
                        r.printRoutingTable();
                    }
                    for (int j = 1; j < 10 ; j++)
                    {
                        ShowRoute s=routes.get(j);
                        System.out.println("Destination::"+j+"---HopCount::"+s.hopCount+"---Path::"+s.path);

                    }
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
                //Display routing path, hop count and routing table of each router [You need to receive
                //all the required info from the server in response to "SHOW_ROUTE" request]
            }
            else
            {
                //Simply send the message and recipient IP address to server.
                packet=new Packet(message,"",own.getIp(),null);
                packet.setSourceNetwork(own.getIp());
                try
                {
                    output.writeObject(packet);
                    System.out.println(input.readObject());
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            //If server can successfully send the message, client will get an acknowledgement along with hop count
            //        Otherwise, client will get a failure message [dropped packet]
        }
        // Report average number of hops and drop rate
        try {
            int dropCount = (int) input.readObject();
            //double averageHopcount=((double)totalHop/(100-dropCount));
            double averageHopcount = (double) input.readObject();

            System.out.println("Drop count is: " + dropCount);
            System.out.println("Average hopcount is: " + averageHopcount);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.out.println("Client ending");

    }
}
