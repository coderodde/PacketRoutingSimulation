package net.coderodde.simulation.network;

import java.util.List;

/**
 * This class implements a packet routing algorithm that computes all-pairs 
 * shortest paths and transmits each packet along the shortest path between the 
 * terminal packet routers.
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.6 (Jul 11, 2016)
 */
public final class ShortestPathPacketRoutingAlgorithm 
extends PacketRoutingAlgorithm {

    @Override
    public SimulationStatistics simulate(List<PacketRouter> network, List<Packet> packetList) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }    
}
