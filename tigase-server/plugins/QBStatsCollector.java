package tigase.xmpp.impl;

import com.quickblox.chat.utils.QBChatUtils;
import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;
import tigase.disteventbus.EventBus;
import tigase.disteventbus.EventBusFactory;
import tigase.server.Message;
import tigase.server.Packet;
import tigase.server.Presence;
import tigase.stats.qb.processors.ProcessPacket;
import tigase.stats.qb.processors.containers.MessageProcessorsContainer;
import tigase.stats.qb.processors.containers.PresenceProcessorsContainer;
import tigase.stats.qb.processors.containers.ProcessorsContainer;
import tigase.xmpp.JID;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.impl.annotation.AnnotatedXMPPProcessor;
import tigase.xmpp.impl.annotation.Handle;
import tigase.xmpp.impl.annotation.Handles;
import tigase.xmpp.impl.annotation.Id;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Logger;

@Id(QBStatsCollector.ID)
@Handles({
        @Handle(
                path = {Message.ELEM_NAME}, xmlns = QBStatsCollector.XMLNS
        ),
        @Handle(
                path = {Presence.ELEM_NAME}, xmlns = QBStatsCollector.XMLNS
        )
})

/**
 * Created by QuickBlox team on 1/1/15.
 */
public class QBStatsCollector extends AnnotatedXMPPProcessor implements XMPPProcessorIfc {
    public static final String ID = "stats_collector";
    public static final String XMLNS = "jabber:client";

    private static final Logger log = Logger.getLogger(QBStatsCollector.class.getName());

    protected EventBus eventBus;
    /**
     * Processors for messages and presence.
     */
    protected Map<String, ProcessorsContainer> processorsContainers;

    @Override
    public void init(Map<String, Object> settings) throws TigaseDBException {
        //init processors
        processorsContainers = new HashMap<>();
        processorsContainers.put(Presence.ELEM_NAME, new PresenceProcessorsContainer());
        processorsContainers.put(Message.ELEM_NAME, new MessageProcessorsContainer());
        //init event bus
        eventBus = EventBusFactory.getInstance();
    }

    @Override
    public void process(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings) throws XMPPException {
        Integer appId = getAppIdFromJID(packet.getStanzaFrom());
        if (appId == null || !packet.getPacketFrom().getLocalpart().equals("c2s")) {
            return;
        }

        ProcessPacket[] processors;
        try {
            //retrieve corresponding processors for incoming element
            processors = processorsContainers.get(packet.getElemName()).getProcessors();
        } catch (Exception e) {
            log.warning("An error occured : " + e.getMessage());
            log.warning("Setting processors to an empty array.");
            processors = new ProcessPacket[0];
        }
        for (ProcessPacket processor : processors) {
            processor.execute(packet, String.valueOf(appId), eventBus);
        }
    }

    protected Integer getAppIdFromJID(JID jid) {
        if (jid == null) {
            log.warning("Can't save stats for Presence or Message because packet.getStanzaFrom() is null");
            return null;
        }
        Integer appId = null;
        Integer[] userParams = QBChatUtils.getApplicationIDAndUserIDFromUserJIDLocalPart(jid.getLocalpart());
        if (userParams != null) {
            appId = userParams[0];
        }
        return appId;
    }
}