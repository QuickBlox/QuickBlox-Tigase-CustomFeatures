package tigase.xmpp.impl;

import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;
import tigase.server.*;
import tigase.server.Message;
import tigase.xmpp.*;
import tigase.xml.Element;
import com.quickblox.chat.utils.QBChatUtils;
import com.quickblox.chat.services.QBChatServices;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by QuickBlox team on 1/1/15.
 */
public class MessageReadDelivered extends XMPPProcessor implements XMPPProcessorIfc {

    private QBChatServices chatServices;

    private static final String ID = "message-read-delivered";
    private static final String  XMLNS  = "urn:xmpp:chat-markers:0";

    public static final String RECEIVED_KEY = "received";
    public static final String DISPLAYED_KEY = "displayed";
    public static final String[] MESSAGE_RECEIVED_PATH = { Message.ELEM_NAME, RECEIVED_KEY};
    public static final String[] MESSAGE_DISPLAYED_PATH = { Message.ELEM_NAME, DISPLAYED_KEY};

    private static final Logger log = Logger.getLogger(MessageReadDelivered.class.getName());

    @Override
    public void init(Map<String, Object> settings) throws TigaseDBException {
        super.init(settings);
        chatServices = QBChatServices.getInstance();
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void process(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings) throws XMPPException {

        // You may want to skip processing completely if the user is offline.
        if (session == null) {
            return;
        }

        BareJID id = (packet.getStanzaFrom() != null) ? packet.getStanzaFrom().getBareJID() : null;

        if (session.isUserId(id) &&  packet.getPacketFrom().getLocalpart().equals("c2s")) {
            JID from = packet.getStanzaFrom();
            Integer[] userParams = QBChatUtils.getApplicationIDAndUserIDFromUserJIDLocalPart(from.getLocalpart());
            if (userParams != null) {
                Integer userId = userParams[1];
                Element received = packet.getElement().getChild(RECEIVED_KEY);
                Element displayed = packet.getElement().getChild(DISPLAYED_KEY);
                String messageDeliveredId = (received != null ? received.getAttributes().get(Packet.ID_ATT) : null);
                String messageReadId = (displayed != null ? displayed.getAttributes().get(Packet.ID_ATT) : null);

                if (messageDeliveredId == null && messageReadId == null) {
                    return;
                }


                if (log.isLoggable(Level.INFO)) {
                    log.log(Level.INFO, "packet: {0}, session: {1}", new Object[] { packet, session != null ? session : ""});
                }

                chatServices.saveMessageStatus(userId, messageReadId, messageDeliveredId);
            }
        }
    }

    @Override
    public String[] supNamespaces() {
        return new String[]{XMLNS, XMLNS};
    }

    @Override
    public String[][] supElementNamePaths() {
        return new String[][]{MESSAGE_RECEIVED_PATH, MESSAGE_DISPLAYED_PATH};
    }
}
