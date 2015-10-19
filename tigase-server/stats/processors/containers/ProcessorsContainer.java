package tigase.stats.qb.processors.containers;

import tigase.stats.qb.processors.ProcessPacket;

/**
 * Aggregates all the desired metrics processors for a certain (defined by implementation) type of packets.
 */
/**
 * Created by QuickBlox team on 1/1/15.
 */
public interface ProcessorsContainer {
    ProcessPacket[] getProcessors();
}
