package net.coderodde.simulation.network;

import java.util.Objects;

/**
 * This class implements a simulated packet being transmitted in the network.
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.61 (Jul 10, 2016)
 */
public final class Packet {

    private final int id;
    private final PacketRouter sourcePacketRouter;
    private final PacketRouter targetPacketRouter;

    public Packet(final int id, 
                  final PacketRouter sourcePacketRouter,
                  final PacketRouter targetPacketRouter) {
        this.id = id;
        this.sourcePacketRouter = 
                Objects.requireNonNull(sourcePacketRouter,
                                       "The source packet router is null.");
        this.targetPacketRouter =
                Objects.requireNonNull(targetPacketRouter,
                                       "The target packet router is null.");

        if (sourcePacketRouter.equals(targetPacketRouter)) {
            throw new IllegalArgumentException(
                    "The source and target routers are same: " +
                    sourcePacketRouter);
        }
    }

    public int getId() {
        return id;
    }

    public PacketRouter getSourcePacketRouter() {
        return sourcePacketRouter;
    }

    public PacketRouter getTargetPacketRouter() {
        return targetPacketRouter;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null) {
            return false;
        }

        if (!getClass().equals(o.getClass())) {
            return false;
        }

        return id == ((Packet) o).id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
