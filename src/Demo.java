
import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.coderodde.simulation.network.Packet;
import net.coderodde.simulation.network.PacketRouter;
import net.coderodde.simulation.network.AbstractPacketRoutingAlgorithm;
import net.coderodde.simulation.network.LearningPacketRoutingAlgorithm;
import net.coderodde.simulation.network.RandomPacketRoutingAlgorithm;
import net.coderodde.simulation.network.ShortestPathPacketRoutingAlgorithm;
import net.coderodde.simulation.network.SimulationStatistics;

public class Demo {
 
    private static final int DEFAULT_NUMBER_OF_ROUTERS = 50;
    private static final int DEFAULT_NUMBER_OF_LINKS   = 350;
    private static final int DEFAULT_NUMBER_OF_PACKETS = 1000;
    
    private static final int MINIMUM_NUMBER_OF_ROUTERS = 1;
    private static final int MINIMUM_NUMBER_OF_LINKS   = 1;
    private static final int MINIMUM_NUMBER_OF_PACKETS = 1;
    
    private static final int BAR_LENGTH = 80;
    private static final char BAR_CHAR = '-';
    private static final String BAR;
    
    static {
        final StringBuilder sb = new StringBuilder(BAR_LENGTH);
        
        for (int i = 0; i < BAR_LENGTH; ++i) {
            sb.append(BAR_CHAR);
        }
        
        BAR = sb.toString();
    }
    
    public static enum ErrorCondition {
        
        BAD_ROUTERS_TOKEN (1),
        TOO_LITTLE_ROUTERS(2),
        
        BAD_LINKS_TOKEN   (3),
        TOO_LITTLE_LINKS  (4),
        
        BAD_PACKETS_TOKEN (5),
        TOO_LITTLE_PACKETS(6),
        
        NETWORK_DISCONNECTED(7);
        
        ErrorCondition(final int returnCode) {
            this.returnCode = returnCode;
        }
        
        public int code() {
            return returnCode;
        }
        
        private final int returnCode;
    }
    
    private static final String USAGE_INFO = 
            "Usage: java -jar This.jar [ROUTERS LINKS PACKETS]\n" +
            "Where: \n" +
            "    ROUTERS the number of routers in the network.\n" +
            "    LINKS   the number of links between routers.\n" +
            "    PACKETS the number of packets to simulate.\n";
    
    public static void main(final String[] args) {
        int routers = DEFAULT_NUMBER_OF_ROUTERS;
        int links   = DEFAULT_NUMBER_OF_LINKS;
        int packets = DEFAULT_NUMBER_OF_PACKETS;
        
        if (args.length != 0) {
            if (args.length != 3) {
                printUsageInfo();
                return;
            }
            
            // Try read the number of routers from the command line.
            try {
                routers = Integer.parseInt(args[0]);
            } catch (final NumberFormatException ex) {
                System.err.println(
                        "ERROR: Cannot parse the number of routers: \"" +
                        args[0] + "\".");
                System.exit(ErrorCondition.BAD_ROUTERS_TOKEN.code());
            }
            
            if (routers < MINIMUM_NUMBER_OF_ROUTERS) {
                System.err.println("ERROR: The number of routers is too " +
                                   "small (" + routers + "). Must be at " +
                                   "least " + MINIMUM_NUMBER_OF_ROUTERS + ".");
                System.exit(ErrorCondition.TOO_LITTLE_ROUTERS.code());
            }
            
            // Try read the number of links from the command line.
            try {
                links = Integer.parseInt(args[1]);
            } catch (final NumberFormatException ex) {
                System.err.println(
                        "ERROR: Cannot parse the number of links: \"" +
                        args[1] + "\".");
                System.exit(ErrorCondition.BAD_LINKS_TOKEN.code());
            }
            
            if (links < MINIMUM_NUMBER_OF_LINKS) {
                System.err.println("ERROR: The number of links is too " +
                                   "small (" + links + "). Must be at " +
                                   "least " + MINIMUM_NUMBER_OF_LINKS + ".");
                System.exit(ErrorCondition.TOO_LITTLE_LINKS.code());
            }
            
            // Try read the number of packets from the command line.
            try {
                packets = Integer.parseInt(args[2]);
            } catch (final NumberFormatException ex) {
                System.err.println(
                        "ERROR: Cannot parse the number of packets: \"" +
                        args[2] + "\".");
                System.exit(ErrorCondition.BAD_PACKETS_TOKEN.code());
            }
            
            if (packets < MINIMUM_NUMBER_OF_PACKETS) {
                System.err.println("ERROR: The number of routers is too " +
                                   "small (" + routers + "). Must be at " +
                                   "least " + MINIMUM_NUMBER_OF_PACKETS + ".");
                System.exit(ErrorCondition.TOO_LITTLE_PACKETS.code());
            }
        }
        
        final long seed = System.nanoTime();
        final Random random = new Random(seed);
        
        System.out.println("[INFO] Seed = " + seed);
        System.out.println("[INFO] Requested number of routers: " + routers);
        System.out.println("[INFO] Requested number of links:   " + links);
        System.out.println("[INFO] Requested number of packets: " + packets);
        System.out.println("[STATUS] Building the network...");
        
        long startTime = System.nanoTime();
        final List<PacketRouter> network = createRandomNetwork(routers,
                                                               links, 
                                                               random);
        long endTime = System.nanoTime();
        
        System.out.printf ("[STATUS] Network build in %.1f milliseconds!\n",
                           (endTime - startTime) / 1e6);
        
        if (!inputNetworkIsConnected(network)) {
            System.err.println(
                    "ERROR: The constructed network is disconnected.");
            System.exit(ErrorCondition.NETWORK_DISCONNECTED.code());
        }
        
        final List<Packet> packetList = createRandomPacketList(network, 
                                                               packets, 
                                                               random);
        final AbstractPacketRoutingAlgorithm algorithm1 = 
                new RandomPacketRoutingAlgorithm();
        
        final LearningPacketRoutingAlgorithm algorithm2 =
                new LearningPacketRoutingAlgorithm();
        
        algorithm2.setCycleLimit(4000);
        
        final AbstractPacketRoutingAlgorithm algorithm3 = 
                new ShortestPathPacketRoutingAlgorithm();
        
        profile(algorithm1, network, packetList);
        profile(algorithm2, network, packetList);
        profile(algorithm3, network, packetList);
    }
    
    private static void profile(final AbstractPacketRoutingAlgorithm algorithm,
                                final List<PacketRouter> network,
                                final List<Packet> packetList) {
        final long startTime = System.nanoTime();
        final SimulationStatistics statistics = algorithm.simulate(network, 
                                                                   packetList);
        final long endTime = System.nanoTime();
        
        System.out.println(BAR);
        System.out.printf(
                "[STATISTICS] Actual simulation time: %.1f milliseconds.\n", 
                (endTime - startTime) / 1e6);
        
        System.out.println("[STATISTICS] Algorithm class: " + 
                           algorithm.getClass().getSimpleName());
        System.out.println("[STATISTICS] Result:");
        System.out.println(statistics);
    }
    
    private static void printUsageInfo() {
        System.out.println(USAGE_INFO);
    }
    
    private static List<Packet> 
        createRandomPacketList(final List<PacketRouter> network,
                               final int numberOfPackets,
                               final Random random) {
        if (network.size() < 2) {
            return new ArrayList<>();
        }

        final List<Packet> packetList = new ArrayList<>(numberOfPackets);

        for (int id = 0; id < numberOfPackets; ++id) {
            final PacketRouter sourcePacketRouter = 
                    network.get(random.nextInt(network.size()));

            PacketRouter targetPacketRouter;

            do {
                targetPacketRouter = 
                        network.get(random.nextInt(network.size()));
            } while (targetPacketRouter.equals(sourcePacketRouter));

            packetList.add(new Packet(id, 
                                      sourcePacketRouter, 
                                      targetPacketRouter));
        }

        return packetList;
    }
    
    private static boolean 
        inputNetworkIsConnected(final List<PacketRouter> network) {
        if (network.isEmpty()) {
            throw new IllegalArgumentException("The input network is empty.");
        }
        
        final Set<PacketRouter> networkAsSet = new HashSet<>(network);
        final Set<PacketRouter> visitedSet = new HashSet<>();
        final Deque<PacketRouter> queue = new ArrayDeque<>();
        
        queue.add(networkAsSet.iterator().next());
        
        while (!queue.isEmpty()) {
            final PacketRouter current = queue.removeFirst();
            
            for (final PacketRouter neighbor : current.getNeighbors()) {
                if (!visitedSet.contains(neighbor)) {
                    visitedSet.add(neighbor);
                    queue.addLast(neighbor);
                }
            }
        }
        
        return visitedSet.size() == networkAsSet.size();
    }    
        
    private static List<PacketRouter> createRandomNetwork(final int routers,
                                                          int links,
                                                          final Random random) {
        final List<PacketRouter> network = new ArrayList<>(routers);
        
        for (int id = 0; id < routers; ++id) {
            network.add(new PacketRouter(id));
        }
        
        final int maximumNumberOfLinksPossible = routers * (routers - 1) / 2;
        
        final List<Point> linkDescriptorList = 
                new ArrayList<>(maximumNumberOfLinksPossible); 
        
        for (int startId = 0; startId < routers; ++startId) {
            for (int endId = startId + 1; endId < routers; ++endId) {
                linkDescriptorList.add(new Point(startId, endId));
            }
        }
        
        if (linkDescriptorList.isEmpty()) {
            // No links possible due to too small number of routers.
            return network;
        }
        
        Collections.<Point>shuffle(linkDescriptorList, random);
        
        links = Math.min(links, maximumNumberOfLinksPossible);
        
        for (int i = 0; i < links; ++i) {
            final Point point = linkDescriptorList.get(i);
            network.get(point.x).connect(network.get(point.y));
        }
        
        return network;
    }
}
