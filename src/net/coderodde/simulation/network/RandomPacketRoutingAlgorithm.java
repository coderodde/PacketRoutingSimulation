package net.coderodde.simulation.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * This class implements a packet routing algorithm that sends each packet to a
 * randomly chosen neighbor packet router.
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.6 (Jul 11, 2016)
 */
public final class RandomPacketRoutingAlgorithm extends PacketRoutingAlgorithm {

    /**
     * This map maps each packet to the ordered list of routers it has visited.
     */
    private Map<Packet, List<PacketRouter>> historyMap;
    
    /**
     * This set holds all the packets that have not reached their destination.
     */
    private Set<Packet> undeliveredPacketSet;
    
    /**
     * This list will contain the length of all packet routers' queue lengths at
     * every network cycle. Later, it will be used for computing statistics.
     */
    private List<Integer> queueLengthList;
    
    /**
     * The random number generator.
     */
    private Random random;
    
    /**
     * Constructs the API entry object.
     */
    public RandomPacketRoutingAlgorithm() {}
    
    /**
     * Constructs the actual state object of this algorithm.
     * 
     * @param dummy ignored. Used for distinction between the public and
     *              non-public constructors.
     */
    private RandomPacketRoutingAlgorithm(final boolean dummy) {
        this.historyMap           = new HashMap<>();
        this.undeliveredPacketSet = new HashSet<>();
        this.queueLengthList      = new ArrayList<>();
        this.random               = new Random();
    }
    
    @Override
    public SimulationStatistics simulate(final List<PacketRouter> network, 
                                         final List<Packet> packetList) {
        final RandomPacketRoutingAlgorithm state = 
                new RandomPacketRoutingAlgorithm(true);
        
        return state.simulateImpl(network, packetList);
    }
    
    private SimulationStatistics simulateImpl(final List<PacketRouter> network,
                                              final List<Packet> packetList) {
        initializePackets(packetList);
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
    
    private static double sd(final List<Integer> list) {
        int sum = 0;
        
        for (final Integer i : list) {
            sum += i;
        }
        
        final double average = 1.0 * sum / list.size();
        double sum2 = 0.0;
        
        for (final Integer i : list) {
            sum2 += (i - average) * (i - average);
        }
        
        return Math.sqrt(sum2 / (list.size() - 1));
    }
    
    private void simulateCycle(final List<PacketRouter> network) {
        final Map<Packet, PacketRouter> map = new HashMap<>();
        
        // Find out to which packet router to send the packets:
        for (final PacketRouter packetRouter : network) {
            if (packetRouter.queueLength() > 0) {
                final Packet packet = packetRouter.dequeuePacket();
                final PacketRouter nextPacketRouter = 
                        choose(packetRouter.getNeighbors(), random);
                map.put(packet, nextPacketRouter);
//                nextPacketRouter.enqueuePacket(packet);
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
    
    private void loadPacketRouterQueueLengths(
            final List<PacketRouter> network) {
        network.forEach((router) -> { 
            queueLengthList.add(router.queueLength()); 
        });
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
        
    private static <T> T lastOf(final List<T> list) {
        return list.get(list.size() - 1);
    }
    
    private static <T> T choose(final List<T> list, final Random random) {
        return list.get(random.nextInt(list.size()));
    }
}
