package tigase.stats.qb.processors.containers;

import tigase.stats.qb.processors.ProcessPacket;
import tigase.stats.qb.processors.message.*;

import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Aggregates all the desired metrics processors for message packets.
 */
/**
 * Created by QuickBlox team on 1/1/15.
 */
public class MessageProcessorsContainer implements ProcessorsContainer {
    private static final Logger log = Logger.getLogger(MessageProcessorsContainer.class.getName());

    /**
     * Metrics processors.
     */
    private static final ProcessPacket[] processors;

    static {
        log.info("MessageProcessorsContainer.static initializer");
        processors = new ProcessPacket[]{
                new ProcessMessage(),
                new ProcessOrdinaryMessage(),
                new ProcessVideoCall(),
                new ProcessChatMarker(),
                new ProcessChatStateNotification(),
                new ProcessAttachment()
        };
        log.info("Finished initializing MessageProcessorsContainer.processors : " + Arrays.toString(processors));
    }

    @Override
    public ProcessPacket[] getProcessors() {
        return processors;
    }
}
