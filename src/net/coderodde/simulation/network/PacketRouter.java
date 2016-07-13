package net.coderodde.simulation.network;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * This class defines a packet router in a simulated network. Each 
 * {@code PacketRouter} maintains a FIFO queue of packets that have been 
 * received but not yet sent away. The routers work in cycles. At each cycle the
 * router may send away at most one packet (provided that the packet queue is 
 * not empty), and receive any number of packets.
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.6 (Jul 10, 2016)
 */
public final class PacketRouter {
   
    /**
     * The ID of the packet router. The IDs must be unique.
     */
    private final int id;
    
    /**
     * The list of packet routers to which there is an undirected link from this
     * packet router.
     */
    private final List<PacketRouter> neighbors = new ArrayList<>();
    
    /**
     * The internal queue of packets not yet emitted.
     */
    private final Deque<Packet> queue = new ArrayDeque<>();
    
    public PacketRouter(final int id) {
        this.id = id;
    }
    
    public final void connect(final PacketRouter neighborPacketRouter) {
        Objects.requireNonNull(neighborPacketRouter,
                               "The input neighbor packet router is null.");
        
        if (!neighbors.contains(neighborPacketRouter)) {
            neighbors.add(neighborPacketRouter);
            neighborPacketRouter.neighbors.add(this);
        }
    }
    
    public final List<PacketRouter> getNeighbors() {
        return Collections.<PacketRouter>unmodifiableList(neighbors);
    }
    
    public Collection<Packet> getQueue() {
        return Collections.<Packet>unmodifiableCollection(queue);
    }
    
    /**
     * Removes the delivered packet from the queue. This {@code PacketRouter} 
     * must be the target packet router of {@code packet}.
     * 
     * @param packet 
     */
    public void remove(final Packet packet) {
        if (!packet.getTargetPacketRouter().equals(this)) {
            throw new IllegalArgumentException(
                    "The input packet is not in its " +
                    "destination packet router.");
        }
        
        queue.remove(packet);
    }
    
    public int queueLength() {
        return queue.size();
    }
    
    public void enqueuePacket(final Packet packet) {
        queue.addLast(packet);
    }
    
    public Packet dequeuePacket() {
        return queue.removeFirst();
    }
    
    @Override
    public boolean equals(final Object o) {
        if (o == null || !getClass().equals(o.getClass())) {
            return false;
        }
        
        return id == ((PacketRouter) o).id;
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        
        sb.append("[PacketRouter: id = ")
          .append(id)
          .append(", neighbors = <");
        
        if (!neighbors.isEmpty()) {
            sb.append(neighbors.get(0).id);
        }
        
        for (int i = 1; i < neighbors.size(); ++i) {
            sb.append(", ").append(neighbors.get(i).id);
        }
        
        sb.append(">, queue = <");
        
        final Iterator<Packet> iterator = queue.iterator();
        
        if (iterator.hasNext()) {
            sb.append(iterator.next().getId());
        }
        
        while (iterator.hasNext()) {
            sb.append(", ").append(iterator.next().getId());
        }
        
        sb.append(">]");
        
        return sb.toString();
    }
}
