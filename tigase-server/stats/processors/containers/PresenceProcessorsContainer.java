package tigase.stats.qb.processors.containers;

import tigase.stats.qb.processors.ProcessPacket;
import tigase.stats.qb.processors.presence.ProcessPresence;

import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Aggregates all the desired metrics processors for presence packets.
 */
/**
 * Created by QuickBlox team on 1/1/15.
 */
public class PresenceProcessorsContainer implements ProcessorsContainer {
    private static final Logger log = Logger.getLogger(PresenceProcessorsContainer.class.getName());

    /**
     * Metrics processors.
     */
    private static final ProcessPacket[] processors;

    static {
        log.info("PresenceProcessorsContainer.static initializer");
        processors = new ProcessPacket[]{
                new ProcessPresence()
        };
        log.info("Finished initializing PresenceProcessorsContainer.processors : " + Arrays.toString(processors));
    }

    @Override
    public ProcessPacket[] getProcessors() {
        return processors;
    }
}
