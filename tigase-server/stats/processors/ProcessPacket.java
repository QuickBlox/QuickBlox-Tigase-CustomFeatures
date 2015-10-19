package tigase.stats.qb.processors;

import tigase.disteventbus.EventBus;
import tigase.server.Packet;

/**
 * Command hierarchy root for packets processing.
 */
/**
 * Created by QuickBlox team on 1/1/15.
 */
public abstract class ProcessPacket {
    /**
     * Fires an event on {@code eventBus} for the given {@code appId},
     * if the appropriate metrics (defined by implementation) is present in {@code packet}.
     *
     * @param packet
     * @param appId
     * @param eventBus
     */
    public abstract void execute(Packet packet, String appId, EventBus eventBus);

    @Override
    public String toString() {
        return this.getClass().getName();
    }
}
