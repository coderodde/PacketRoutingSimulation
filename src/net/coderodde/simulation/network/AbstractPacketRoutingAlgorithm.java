package net.coderodde.simulation.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * This class defines the API and utility methods of a packet routing algorithm.
 * The simulation rules are as follows:
 * 
 * <ul>
 * <li>The network works in "cycles,"</li>
 * <li>During each cycle, each packet router may send at most one packet to 
 *     its neighbor, yet is allowed to receive any number of incoming packets,
 * </li>
 * <li>The packet queue of each packet router is FIFO.</li>
 * </ul>
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.6 (Jul 18, 2016)
 */
public abstract class AbstractPacketRoutingAlgorithm {
    
    /**
     * This map maps each packet to its transmission history. The history of
     * each packet is the list of packet routers that the packet had to visit in
     * order to reach the destination. If a packet spends more than one 
     * consecutive "network cycle" in a router, it is reflected in the history
     * list by storing the router ID multiple times in a row.
     * <p>
     * For example, if a packet starts from router ID 3, spends two cycles in 
     * the router with ID 5 and finally reaches its destination (router ID 1),
     * the history list would contain <code><3, 5, 5, 1></code>.
     */
    protected Map<Packet, List<PacketRouter>> historyMap;
    
    /**
     * While simulation is running, this set contains only those packets that
     * have not yet reached their respective targets.
     */
    protected Set<Packet> undeliveredPacketSet;
    
    /**
     * This list stores all the queue length in all packet routers at all 
     * network cycles.
     */
    protected List<Integer> queueLengthList;
    
    /**
     * The number of network cycles made in a network. Starts form one as we 
     * count network initialization as well.
     */
    protected int cycles = 1;
    
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
        
        
    protected SimulationStatistics buildStatistics() {   
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
    
    protected void initializePackets(final List<Packet> packetList) {
        for (final Packet packet : packetList) {
            packet.getSourcePacketRouter().enqueuePacket(packet);
            
            historyMap.put(packet,
                           new ArrayList<>(
                                   Arrays.asList(
                                           packet.getSourcePacketRouter())));
        }
    }
        
    protected void loadPacketRouterQueueLengths(
            final List<PacketRouter> network) {
        network.forEach((router) -> { 
            queueLengthList.add(router.queueLength()); 
        });
    }
    
    protected void pruneDeliveredPackets() {
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
        
    protected static <T> T lastOf(final List<T> list) {
        return list.get(list.size() - 1);
    }
    
    protected static <T> T choose(final List<T> list, final Random random) {
        return list.get(random.nextInt(list.size()));
    }
}
