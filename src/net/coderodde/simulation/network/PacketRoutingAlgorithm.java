package net.coderodde.simulation.network;

import java.util.List;

/**
 * This class defines the API and utility methods of a packet routing algorithm.
 * 
 * @author Rodion "rodde" Efremov
 */
public abstract class PacketRoutingAlgorithm {
    
    /**
     * Runs a packet routing algorithm and returns the statistics of a 
     * simulation run.
     * 
     * @param network    the list of packet routers comprising the network.
     * @param packetList the list of packets to deliver.
     * @return the object holding the statistical results of the simulation.
     */
    public abstract SimulationStatistics 
        simulate(final List<PacketRouter> network,
                 final List<Packet> packetList);
}
