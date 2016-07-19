package net.coderodde.simulation.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * This class implements a packet routing algorithm that sends each packet to a
 * randomly chosen neighbor packet router.
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.6 (Jul 11, 2016)
 */
public final class RandomPacketRoutingAlgorithm extends AbstractPacketRoutingAlgorithm {

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
        
        undeliveredPacketSet.addAll(packetList);
        
        while (!undeliveredPacketSet.isEmpty()) {
            loadPacketRouterQueueLengths(network);
            simulateCycle(network);
            pruneDeliveredPackets();
            ++cycles;
        }
        
        return buildStatistics();
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
}
