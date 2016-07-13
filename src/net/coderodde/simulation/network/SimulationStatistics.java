package net.coderodde.simulation.network;

/**
 * This class holds statistical results of a simulation. The data includes
 * <ul>
 *   <li>the minimum queue length,</li>
 *   <li>the maximum queue length,</li>
 *   <li>the average queue length,</li>
 *   <li>the standard deviation of the queue lengths.</li>
 * </ul>
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.6 (Jul 10, 2016)
 */
public class SimulationStatistics {
    
    private final int minimumQueueLength;
    private final int maximumQueueLength;
    private final double averageQueueLength;
    private final double queueLengthStandardDeviation;
    
    private final int minimumTransmissionDuration;
    private final int maximumTransmissionDuration;
    private final double averageTransmissionDuration;
    private final double transmissionDurationStandardDeviation;
    
    private final int networkCycles;
    
    SimulationStatistics(final int minimumQueueLength,
                         final int maximumQueueLength,
                         final double averageQueueLength,
                         final double queueLengthStandardDeviation,
                         final int minimumTransmissionDuration,
                         final int maximumTransmissionDuration,
                         final double averageTransmissionDuration,
                         final double transmissionDurationStandardDeviation,
                         final int networkCycles) {
        this.minimumQueueLength = minimumQueueLength;
        this.maximumQueueLength = maximumQueueLength;
        this.averageQueueLength = averageQueueLength;
        this.queueLengthStandardDeviation = queueLengthStandardDeviation;
        this.minimumTransmissionDuration = minimumTransmissionDuration;
        this.maximumTransmissionDuration = maximumTransmissionDuration;
        this.averageTransmissionDuration = averageTransmissionDuration;
        this.transmissionDurationStandardDeviation =
                transmissionDurationStandardDeviation;
        this.networkCycles = networkCycles;
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        
        // Packet queue statistics:
        
        sb.append("Minimum queue length:          ")
          .append(minimumQueueLength)
          .append("\n");
        
        sb.append("Maximum queue length:          ")
          .append(maximumQueueLength)
          .append("\n");
        
        sb.append("Average queue length:          ")
          .append(averageQueueLength)
          .append("\n");
        
        sb.append("Queue length s.d.:             ")
          .append(queueLengthStandardDeviation)
          .append("\n");
        
        // Delivery time statistics:
        
        sb.append("Minimum transmission duration: ")
          .append(minimumTransmissionDuration)
          .append("\n");
        
        sb.append("Maximum transmission duration: ")
          .append(maximumTransmissionDuration)
          .append("\n");
        
        sb.append("Average transmission duration: ")
          .append(averageTransmissionDuration)
          .append("\n");
        
        sb.append("Transmission duration s.d.:    ")
          .append(transmissionDurationStandardDeviation)
          .append("\n");
        
        sb.append("Total network cycles:          ")
          .append(networkCycles);
        
        return sb.toString();
    }
}
