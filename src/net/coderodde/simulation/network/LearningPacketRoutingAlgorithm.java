package net.coderodde.simulation.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * This class implements a packet routing algorithm that learns shorter paths
 * during its operation: whenever a packet is received, it checks its history to
 * see whether it is possible to improve the paths. For example, suppose the 
 * history of a received packet is <tt>n_1, n_2, ..., n_k</tt>. For each packet
 * router <tt>n_1, n_2, ..., n_k</tt> the algorithm checks whether the current
 * path improves the path lengths starting from the routers in question, and if
 * so, updates its state to further use those improved path, at least until they
 * may be improved even further.
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.6 (Jul 11, 2016)
 */
public final class LearningPacketRoutingAlgorithm 
extends AbstractPacketRoutingAlgorithm {

    private Map<PacketRouter, Map<PacketRouter, PacketRouter>> dispatchTable;
    private Map<PacketRouter, Map<PacketRouter, Integer>> distanceTable;
    private Random random;
    
    public LearningPacketRoutingAlgorithm() {}
    
    private LearningPacketRoutingAlgorithm(final boolean dummy) {
        this.historyMap           = new HashMap<>();
        this.undeliveredPacketSet = new HashSet<>();
        this.queueLengthList      = new ArrayList<>();
        this.dispatchTable        = new HashMap<>();
        this.distanceTable        = new HashMap<>();
        this.random               = new Random();
    }
    
    @Override
    public SimulationStatistics simulate(final List<PacketRouter> network,
                                         final List<Packet> packetList) {
        final LearningPacketRoutingAlgorithm state =
                new LearningPacketRoutingAlgorithm(true);
        
        return state.simulate(network, packetList);
    }
    
    private SimulationStatistics simulateImpl(final List<PacketRouter> network,
                                              final List<Packet> packetList) {
        initializePackets(packetList);
        buildDispatchTable(network);
        
        int cycles = 0;
        
        undeliveredPacketSet.addAll(packetList);
        
        while (!undeliveredPacketSet.isEmpty()) {
            loadPacketRouterQueueLengths(network);
            simulateCycle(network);
            pruneDeliveredPackets();
            ++cycles;
        }
        
        return buildStatistics(cycles);
    }
    
    private void buildDispatchTable(final List<PacketRouter> network) {
        // Initialize the distance table:
        for (final PacketRouter source : network) {
            final Map<PacketRouter, Integer> localDistanceTable = 
                    new HashMap<>(network.size());
            
            distanceTable.put(source, localDistanceTable);
            
            for (final PacketRouter target : network) {
                if (!target.equals(source)) {
                    localDistanceTable.put(target, Integer.MAX_VALUE);
                }
            }
        }
        
        // Randomly initialize the dispatch table:
        for (final PacketRouter source : network) {
            final Map<PacketRouter, PacketRouter> localDispatchTable = 
                    new HashMap<>();
            
            dispatchTable.put(source, localDispatchTable);
            
            for (final PacketRouter target : network) {
                if (!target.equals(source)) {
                    PacketRouter nextRouter;
                    
                    do {
                        nextRouter = choose(network, random);
                    } while (!nextRouter.equals(source));
                    
                    localDispatchTable.put(target, nextRouter);
                }
            }
        }
    }
    
    private void simulateCycle(final List<PacketRouter> network) {
        
    }
}
