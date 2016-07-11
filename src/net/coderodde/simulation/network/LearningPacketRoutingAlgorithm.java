package net.coderodde.simulation.network;

import java.util.List;

/**
 * This class implements a packet routing algorithm that learns shorter paths
 * during its operation: whenever a packet is received, it checks its history to
 * see whether it is possible to improve the paths. For example, suppose the 
 * history of a received packet is <tt>n_1, n_2, ..., n_k</tt>. For each packet
 * router <tt>n_1, n_2, ..., n_k</tt> the algorithm checks whether the current
 * path improves the path lengths starting from the routers in question, and if
 * so, updates its state to further use those improved path, at least until they
 * may be improved.
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.6 (Jul 11, 2016)
 */
public final class LearningPacketRoutingAlgorithm 
extends PacketRoutingAlgorithm {

    @Override
    public SimulationStatistics simulate(List<PacketRouter> network, List<Packet> packetList) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
