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
    private int cycleLimit;
    
    public LearningPacketRoutingAlgorithm() {}
    
    private LearningPacketRoutingAlgorithm(final int cycleLimit) {
        this.historyMap           = new HashMap<>();
        this.undeliveredPacketSet = new HashSet<>();
        this.queueLengthList      = new ArrayList<>();
        this.dispatchTable        = new HashMap<>();
        this.distanceTable        = new HashMap<>();
        this.random               = new Random();
        this.cycleLimit           = cycleLimit;
    }
    
    public void setCycleLimit(final int cycleLimit) {
        this.cycleLimit = cycleLimit;
    }
    
    @Override
    public SimulationStatistics simulate(final List<PacketRouter> network,
                                         final List<Packet> packetList) {
        final LearningPacketRoutingAlgorithm state =
                new LearningPacketRoutingAlgorithm(cycleLimit);
        
        return state.simulateImpl(network, packetList);
    }
    
    private SimulationStatistics simulateImpl(final List<PacketRouter> network,
                                              final List<Packet> packetList) {
        initializePackets(packetList);
        buildDispatchTable(network);
        
        undeliveredPacketSet.addAll(packetList);
        
        while (!undeliveredPacketSet.isEmpty()) {
            loadPacketRouterQueueLengths(network);
            simulateCycle(network);
            relearnDispatchTable(network);
            pruneDeliveredPackets();
            ++cycles;
            
            if (cycleLimit != 0) {
                if (cycles > cycleLimit) {
                    clearNetwork(network);
                    return null;
                }
            }
        }
        
        return buildStatistics();
    }
    
    private void clearNetwork(final List<PacketRouter> network) {
        for (final PacketRouter packetRouter : network) {
            packetRouter.clearQueue();
        }
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
                    } while (nextRouter.equals(source));
                    
                    localDispatchTable.put(target, nextRouter);
                }
            }
        }
    }
    
    private void simulateCycle(final List<PacketRouter> network) {
        final Map<Packet, PacketRouter> map = new HashMap<>();
        
        // Find out to which packet routers to send the packets:
        for (final PacketRouter packetRouter : network) {
            if (packetRouter.queueLength() > 0) {
                final Packet packet = packetRouter.dequeuePacket();
                final PacketRouter targetRouterOfPacket = 
                        packet.getTargetPacketRouter();
                
                final PacketRouter nextPacketRouter = 
                        dispatchTable.get(packetRouter)
                                     .get(targetRouterOfPacket);
                
                map.put(packet, nextPacketRouter);
            }
        }
        
        // Send the packets:
        for (final Map.Entry<Packet, PacketRouter> entry : map.entrySet()) {
            final Packet packet = entry.getKey();
            final PacketRouter packetRouter = entry.getValue();
            packetRouter.enqueuePacket(packet);
        }
        
        // Update the history of each packet.
        for (final PacketRouter packetRouter : network) {
            for (final Packet packet : packetRouter.getQueue()) {
                historyMap.get(packet).add(packetRouter);
            }
        }
    }
    
    private void relearnDispatchTable(final List<PacketRouter> network) {
        for (final PacketRouter packetRouter : network) {
            final Map<PacketRouter, PacketRouter> localDispatchTable = 
                    dispatchTable.get(packetRouter);
            
            final Map<PacketRouter, Integer> localDistanceTable =
                    distanceTable.get(packetRouter);
            
            final List<Packet> queue = new ArrayList<>(packetRouter.getQueue());
            
            for (final Packet packet : queue) {
                final List<PacketRouter> history = historyMap.get(packet);
                final List<PacketRouter> compressedHistory = compress(history);
                
                if (compressedHistory.size() >= 3) {
                    final int length = compressedHistory.size();
                    final PacketRouter probe1 = compressedHistory
                            .get(length - 3);
                    
                    final PacketRouter probe2 = compressedHistory
                            .get(length - 2);
                    
                    final PacketRouter probe3 = compressedHistory
                            .get(length - 1);
                    
                    if (probe1.equals(probe3) && !probe2.equals(probe1)) {
                        for (int i = 0; i < compressedHistory.size(); ++i) {
                            final PacketRouter pr = compressedHistory.get(i);
                            
                            if (!packetRouter.equals(pr)) {
                                localDistanceTable.put(pr, Integer.MAX_VALUE);
                                localDispatchTable.put(pr, choose(network, random));
                            }
                        }
                    } else {
                        for (int i = 0; i < compressedHistory.size(); ++i) {
                            final PacketRouter pr = compressedHistory.get(i);

                            if (!packetRouter.equals(pr)) {
                                final int distance = compressedHistory.size() - i - 1;

                                if (localDistanceTable.get(pr) > distance) {
                                    localDistanceTable.put(pr, distance);
                                    localDispatchTable.put(
                                            pr, 
                                            compressedHistory.get(
                                                    compressedHistory.size() - 2));
                                }
                            }
                        }
                    }
                } else {
                    for (int i = 0; i < compressedHistory.size(); ++i) {
                        final PacketRouter pr = compressedHistory.get(i);

                        if (!packetRouter.equals(pr)) {
                            final int distance = compressedHistory.size() - i - 1;

                            if (localDistanceTable.get(pr) > distance) {
                                localDistanceTable.put(pr, distance);
                                localDispatchTable.put(
                                        pr, 
                                        compressedHistory.get(
                                                compressedHistory.size() - 2));
                            }
                        }
                    }
                }
            }
        }
    }
    
    public static List<PacketRouter> compress(final List<PacketRouter> history) {
        final List<PacketRouter> compressedHistory = 
                new ArrayList<>(history.size());
        
        PacketRouter previous = history.get(0);
        compressedHistory.add(previous);
        
        for (int i = 1; i < history.size(); ++i) {
            final PacketRouter current = history.get(i);
            
            if (!current.equals(previous)) {
                compressedHistory.add(current);
                previous = current;
            }
        }
        
        return compressedHistory;
    }
}
