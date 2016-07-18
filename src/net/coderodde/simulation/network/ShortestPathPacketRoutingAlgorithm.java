package net.coderodde.simulation.network;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
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
     * This map implements the dispatch table. It maps each packet router 
     * <tt>R</tt> in the network to the next router this router should 
     * send its packet to in order to keep the emitted packet on a shortest path 
     * from this router to <tt>R</tt>
     */
    private Map<PacketRouter, PacketRouter> dispatchTable;
    
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
    }
    
    private void buildDispatchTable(final List<PacketRouter> network) {
        for (final PacketRouter source : network) {
            final Map<PacketRouter, PacketRouter> parentMap = 
                    runBreadthFirstSearchFrom(source);
            
            for (final PacketRouter target : network) {
                if (target.equals(source)) {
                    // Trivial path (source -> source) does not have to be
                    // considered.
                    continue;
                }
                
                final List<PacketRouter> shortestPath = 
                        constructPath(target, parentMap);
                
                // shortestPath.get(0) is the source;
                // shortestPath.get(shortestPath.size() - 1) == target;
                // If we want to reach 'target' from 'source' via a shortest 
                // path, we should move from 'source' to 
                // 'shortestPath.get(1)'.
                dispatchTable.put(target, shortestPath.get(1));
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
}
