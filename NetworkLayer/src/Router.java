/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;

/**
 *
 * @author samsung
 */
public class Router implements Serializable {
    private int routerId;
    private int numberOfInterfaces;
    private ArrayList<IPAddress> interfaceAddrs;//list of IP address of all interfaces of the router
    private ArrayList<RoutingTableEntry> routingTable;//used to implement DVR
    private ArrayList<Integer> neighborRouterIds;//Contains both "UP" and "DOWN" state routers
    private Boolean state;//true represents "UP" state and false is for "DOWN" state

    public boolean change=false;

    public Router() {
        interfaceAddrs = new ArrayList<>();
        routingTable = new ArrayList<>();
        neighborRouterIds = new ArrayList<>();
        
        /**
         * 80% Probability that the router is up
         */

        Random random = new Random();
        double p = random.nextDouble();
        if(p<=0.80) state = true;
        else state = false;

        state=true;

        numberOfInterfaces = 0;
    }
    
    public Router(int routerId, ArrayList<Integer> neighborRouters, ArrayList<IPAddress> interfaceAddrs)
    {
        this.routerId = routerId;
        this.interfaceAddrs = interfaceAddrs;
        this.neighborRouterIds = neighborRouters;
        routingTable = new ArrayList<>();
        
        /**
         * 80% Probability that the router is up
         */
        //state=true;

        Random random = new Random();
        double p = random.nextDouble();
        if(p<=0.80) state = true;
        else state = false;

        state=true;

        numberOfInterfaces = this.interfaceAddrs.size();
    }

    @Override
    public String toString() {
        String temp = "";
        temp+="Router ID: "+routerId+"\n";
        temp+="Intefaces: \n";
        for(int i=0;i<numberOfInterfaces;i++)
        {
            temp+=interfaceAddrs.get(i).getString()+"\t";
        }
        temp+="\n";
        temp+="Neighbors: \n";
        for(int i=0;i<neighborRouterIds.size();i++)
        {
            temp+=neighborRouterIds.get(i)+"\t";
        }
        return temp;
    }

    void printRoutingTable()
    {
        System.out.println("------Routing table-------");
        System.out.println("Destination----Distance----Nexthop");
        for (int i = 0; i <routingTable.size() ; i++)
        {
            System.out.println(routingTable.get(i).getRouterId()+"----"+
                    routingTable.get(i).getDistance()+"----"+routingTable.get(i).getGatewayRouterId());
        }
    }
    
    
    
    /**
     * Initialize the distance(hop count) for each router.
     * for itself, distance=0; for any connected router with state=true, distance=1; otherwise distance=Constants.INFTY;
     */
    public void initiateRoutingTable()
    {
        //routingTable.add(new RoutingTableEntry(10,10,10));
        ArrayList<Router> routers=NetworkLayerServer.routers;
        //ArrayList<EndDevice> Clients=NetworkLayerServer.clientList;
        for (int i = 0; i < 9 ; i++)
        {
            if(i+1==routerId)
            {
                routingTable.add(new RoutingTableEntry(i+1,0,i+1));
            }
            else if(neighborRouterIds.contains(i+1) && routers.get(i).state)
            {
                routingTable.add(new RoutingTableEntry(i+1,1,i+1));
            }
            else
            {
                routingTable.add(new RoutingTableEntry(i+1,Constants.INFTY,0));
            }

        }
        
    }
    
    /**
     * Delete all the routingTableEntry
     */
    public void clearRoutingTable()
    {
        routingTable.clear();
    }
    
    /**
     * Update the routing table for this router using the entries of Router neighbor
     * @param neighbor 
     */
    public boolean updateRoutingTable(Router neighbor)
    {
        if(!neighbor.state&&routingTable.size()>0)
        {
            routingTable.get(neighbor.routerId-1).setDistance(Constants.INFTY);
            routingTable.get(neighbor.routerId-1).setGatewayRouterId(0);
            for (int i = 0; i < routingTable.size() ; i++)
            {
                RoutingTableEntry cEntry=routingTable.get(i);
                if(cEntry.getGatewayRouterId()==neighbor.routerId&& cEntry.getDistance()<Constants.INFTY)
                {
                    cEntry.setDistance(Constants.INFTY);
                    cEntry.setGatewayRouterId(0);
                    change=true;
                }

            }
        }
        else
        {
            ArrayList<RoutingTableEntry> neighboursTable = neighbor.routingTable;
            for (int i = 0; i < routingTable.size(); i++)
            {
                RoutingTableEntry cEntry = routingTable.get(i);
                RoutingTableEntry nEntry = neighboursTable.get(i);
                double d = 1 + nEntry.getDistance();
                if (cEntry.getGatewayRouterId() == neighbor.routerId)
                {
                    if(d==cEntry.getDistance())
                        continue;
                    routingTable.get(i).setDistance(d);
                    routingTable.get(i).setGatewayRouterId(neighbor.routerId);
                    change=true;
                }
                else if (d < routingTable.get(i).getDistance() && nEntry.getGatewayRouterId() != routerId)
                {
                    routingTable.get(i).setDistance(d);
                    routingTable.get(i).setGatewayRouterId(neighbor.routerId);
                    change=true;
                }
                else
                {
                    System.out.println("No changes in entry " + (i+1));

                }

            }
        }
        if(change)
        {
            change=false;
            return true;
        }
        else
            return change;
    }
    //simple dvr update routing table
    public boolean updateRoutingTableSimple(Router neighbor)
    {
        if(!neighbor.state&&routingTable.size()>0)
        {
            routingTable.get(neighbor.routerId-1).setDistance(Constants.INFTY);
            routingTable.get(neighbor.routerId-1).setGatewayRouterId(0);
        }
        else
        {
            ArrayList<RoutingTableEntry> neighboursTable = neighbor.routingTable;
            for (int i = 0; i < routingTable.size(); i++)
            {
                RoutingTableEntry cEntry = routingTable.get(i);
                RoutingTableEntry nEntry = neighboursTable.get(i);
                double d = 1 + nEntry.getDistance();
                if (d < routingTable.get(i).getDistance())
                {
                    routingTable.get(i).setDistance(d);
                    routingTable.get(i).setGatewayRouterId(neighbor.routerId);
                    change=true;
                }
                else
                {
                    System.out.println("No changes in entry " + (i+1));

                }

            }
        }
        if(change)
        {
            change=false;
            return true;
        }
        else
            return change;
    }
    
    /**
     * If the state was up, down it; if state was down, up it
     */
    public void revertState()
    {
        state=!state;
        if(state==true) this.initiateRoutingTable();
        else this.clearRoutingTable();
    }
    
    public int getRouterId() {
        return routerId;
    }

    public void setRouterId(int routerId) {
        this.routerId = routerId;
    }

    public int getNumberOfInterfaces() {
        return numberOfInterfaces;
    }

    public void setNumberOfInterfaces(int numberOfInterfaces) {
        this.numberOfInterfaces = numberOfInterfaces;
    }

    public ArrayList<IPAddress> getInterfaceAddrs() {
        return interfaceAddrs;
    }

    public void setInterfaceAddrs(ArrayList<IPAddress> interfaceAddrs) {
        this.interfaceAddrs = interfaceAddrs;
        numberOfInterfaces = this.interfaceAddrs.size();
    }

    public ArrayList<RoutingTableEntry> getRoutingTable() {
        return routingTable;
    }

    public void addRoutingTableEntry(RoutingTableEntry entry) {
        this.routingTable.add(entry);
    }

    public ArrayList<Integer> getNeighborRouterIds() {
        return neighborRouterIds;
    }

    public void setNeighborRouterIds(ArrayList<Integer> neighborRouterIds) {
        this.neighborRouterIds = neighborRouterIds;
    }

    public Boolean getState() {
        return state;
    }

    public void setState(Boolean state) {
        this.state = state;
    }
    
    
}
