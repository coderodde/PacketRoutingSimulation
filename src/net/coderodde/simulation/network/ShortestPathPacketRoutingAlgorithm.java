package net.coderodde.simulation.network;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

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

    private Map<Packet, List<PacketRouter>> historyMap;
    
    private Set<Packet> undeliveredPacketSet;
    
    private List<Integer> queueLengthList;
    
    private Random random;
    
    /**
     * This map implements the dispatch table. It maps each source packet router
     * <tt>S</tt>, to a partial dispatch table <tt>T(S)</tt>. Each <tt>T(S)</tt>
     * maps each target packet router <tt>D</tt> to a neighbor router <tt>N<tt> 
     * such that <tt>N</tt> is on a shortest path between <tt>S</tt> and 
     * <tt>D</tt>.
     */
    private Map<PacketRouter, Map<PacketRouter, PacketRouter>> dispatchTable;
    
    public ShortestPathPacketRoutingAlgorithm() {}
    
    private ShortestPathPacketRoutingAlgorithm(final boolean dummy) {
        this.historyMap = new HashMap<>();
        this.undeliveredPacketSet = new HashSet<>();
        this.queueLengthList = new ArrayList<>();
        this.random = new Random();
        this.dispatchTable = new HashMap<>();
    }
    
    @Override
    public SimulationStatistics simulate(final List<PacketRouter> network, 
                                         final List<Packet> packetList) {
        final ShortestPathPacketRoutingAlgorithm state = 
                new ShortestPathPacketRoutingAlgorithm(true);
        
        return state.simulateImpl(network, packetList);
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
            pruneDeliveredPackets(undeliveredPacketSet);
            ++cycles;
        }
            
        return buildStatistics(cycles);
    }
    private void simulateCycle(final List<PacketRouter> network) {
        final Map<Packet, PacketRouter> map = new HashMap<>();
        
        // Find out to which packet router to send the packets:
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
    
    private SimulationStatistics buildStatistics(final int cycles) {   
        int minQueueLength = queueLengthList.get(0);
        int maxQueueLength = queueLengthList.get(0);
        
        int queueLengthSum = 0;
        int squaredQueueLengthSum = 0;
        
        for (final int i : queueLengthList) {
            if (minQueueLength > i) {
                minQueueLength = i;
            } else if (maxQueueLength < i) {
                maxQueueLength = i;
            }
            
            queueLengthSum += i;
            squaredQueueLengthSum += i * i;
        }
        
        
        final double queueLengthAverage = 
                1.0 * queueLengthSum / queueLengthList.size();
        
//        final double queueLengthSd = sd(queueLengthList);
        final double queueLengthSd = 
                Math.sqrt(
                    (1.0 * squaredQueueLengthSum - 
                     1.0 * queueLengthSum * queueLengthSum / 
                           queueLengthList.size()) 
                  / (queueLengthList.size() - 1)
                );
        
        int minHistoryLength = historyMap.values().iterator().next().size();
        int maxHistoryLength = minHistoryLength;
        
        int historyLengthSum = 0;
        int squaredHistoryLengthSum = 0;
        
        for (final List<PacketRouter> history : historyMap.values()) {
            final int length = history.size();
            
            if (minHistoryLength > length) {
                minHistoryLength = length;
            } else if (maxHistoryLength < length) {
                maxHistoryLength = length;
            }
            
            historyLengthSum += length;
            squaredHistoryLengthSum += length * length;
        }
        
        final double historyLengthAverage = 
                1.0 * historyLengthSum / historyMap.size();
        
        final double historyLengthSd = 
                Math.sqrt(
                    (1.0 * squaredHistoryLengthSum -
                     1.0 * historyLengthSum * historyLengthSum / 
                           historyMap.size())
                  / (historyMap.size() - 1)
                );
        
        return new SimulationStatistics(minQueueLength,
                                        maxQueueLength,
                                        queueLengthAverage,
                                        queueLengthSd,
                                        minHistoryLength,
                                        maxHistoryLength,
                                        historyLengthAverage,
                                        historyLengthSd,
                                        cycles);
    }
    
    private void 
        pruneDeliveredPackets(final Set<Packet> undeliveredPacketSet) {
        final Iterator<Packet> iterator = undeliveredPacketSet.iterator();
        
        while (iterator.hasNext()) {
            final Packet packet = iterator.next();
            final PacketRouter targetOfPacket = packet.getTargetPacketRouter();
            final List<PacketRouter> historyOfPacket = historyMap.get(packet);
            
            if (lastOf(historyOfPacket).equals(targetOfPacket)) {
                iterator.remove();
                targetOfPacket.remove(packet);
            }
        }
    }
        
    private void loadPacketRouterQueueLengths(
            final List<PacketRouter> network) {
        network.forEach((router) -> { 
            queueLengthList.add(router.queueLength()); 
        });
    }
    
    private void buildDispatchTable(final List<PacketRouter> network) {
        for (final PacketRouter source : network) {
            // Create the local dispatch table for the packet router 'source':
            final Map<PacketRouter, PacketRouter> parentMap = 
                    runBreadthFirstSearchFrom(source);
            
            final Map<PacketRouter, PacketRouter> localDispatchTable =
                    new HashMap<>();
            
            dispatchTable.put(source, localDispatchTable);
            
            for (final PacketRouter target : network) {
                if (target.equals(source)) {
                    // Trivial path (source -> source) does not have to be
                    // considered.
                    continue;
                }
                
                final List<PacketRouter> shortestPath = 
                        constructPath(target, parentMap);
                
                // shortestPath.get(0) is the source;
                // shortestPath.get(shortestPath.size() - 1) is the target;
                // If we want to reach 'target' from 'source' via a shortest 
                // path, we should move from 'source' to 
                // 'shortestPath.get(1)'.
                localDispatchTable.put(target, shortestPath.get(1));
            }
        }
    }
    
    private Map<PacketRouter, PacketRouter> 
        runBreadthFirstSearchFrom(final PacketRouter source) {
        final Deque<PacketRouter> queue = 
                new ArrayDeque<>(Arrays.asList(source));
        final Map<PacketRouter, PacketRouter> parentMap = new HashMap<>();
        parentMap.put(source, null);
        
        while (!queue.isEmpty()) {
            final PacketRouter current = queue.removeFirst();
            
            for (final PacketRouter neighbor : current.getNeighbors()) {
                if (!parentMap.containsKey(neighbor)) {
                    parentMap.put(neighbor, current);
                    queue.addLast(neighbor);
                }
            }
        }
        
        return parentMap;
    }
        
    private List<PacketRouter> constructPath(
            final PacketRouter target,
            final Map<PacketRouter, PacketRouter> parentMap) {
        final List<PacketRouter> path = new ArrayList<>();
        PacketRouter current = target;
        
        while (current != null) {
            path.add(current);
            current = parentMap.get(current);
        }
        
        Collections.<PacketRouter>reverse(path);
        return path;
    }
    
    private void initializePackets(final List<Packet> packetList) {
        for (final Packet packet : packetList) {
            packet.getSourcePacketRouter().enqueuePacket(packet);
            
            historyMap.put(packet,
                           new ArrayList<>(
                                   Arrays.asList(
                                           packet.getSourcePacketRouter())));
        }
    }
        
    private static <T> T lastOf(final List<T> list) {
        return list.get(list.size() - 1);
    }
    
}
