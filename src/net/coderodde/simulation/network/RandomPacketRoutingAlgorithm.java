package net.coderodde.simulation.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
    private final Map<Packet, List<PacketRouter>> historyMap;
    
    /**
     * This set holds all the packets that have not reached their destination.
     */
    private final Set<Packet> undeliveredPacketSet;
    
    /**
     * Constructs the API entry object.
     */
    public RandomPacketRoutingAlgorithm() {
        this.historyMap = null;
        this.undeliveredPacketSet = null;
    }
    
    /**
     * Constructs the actual state object of this algorithm.
     * 
     * @param dummy ignored. Used for distinction between the public and
     *              non-public constructors.
     */
    private RandomPacketRoutingAlgorithm(final boolean dummy) {
        this.historyMap = new HashMap<>();
        this.undeliveredPacketSet = new HashSet<>();
    }
    
    @Override
    public SimulationStatistics simulate(final List<PacketRouter> network, 
                                         final List<Packet> packetList) {
        final RandomPacketRoutingAlgorithm state = 
                new RandomPacketRoutingAlgorithm(true);
        
        state.initializePackets(packetList);
        
        while (!undeliveredPacketSet.isEmpty()) {
            
            pruneDeliveredPackets(undeliveredPacketSet);
        }
        
        return null;
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
            }
        }
    }
        
    private static <T> T lastOf(final List<T> list) {
        return list.get(list.size() - 1);
    }
}
