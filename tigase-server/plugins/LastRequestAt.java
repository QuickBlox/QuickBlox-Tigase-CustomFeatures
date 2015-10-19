package main.java.com.quickblox.plugin.lastrequestat;

import tigase.db.*;
import tigase.db.jdbc.QBAuth;
import tigase.server.Packet;
import tigase.server.Presence;
import tigase.xmpp.*;

import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by QuickBlox team on 1/1/15.
 */
public class LastRequestAt extends XMPPProcessor
        implements XMPPProcessorIfc {

    private static final String     ID        = "lastrequestat";

    private static final String     ELEM_NAME = Presence.ELEM_NAME;
    private static final String[][] ELEMENTS  = {
            { ELEM_NAME }
    };

    private static final String   XMLNS  = "jabber:client";
    private static final String[] XMLNSS = { XMLNS };

    private static final Logger log    = Logger.getLogger(LastRequestAt.class.getName());

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void init(Map<String, Object> settings) throws TigaseDBException {
        super.init(settings);
    }

    @Override
    public String[][] supElementNamePaths() {
        return ELEMENTS;
    }

    @Override
    public String[] supNamespaces() {
        return XMLNSS;
    }

    @Override
    public void process(Packet packet, XMPPResourceConnection xmppResourceConnection, NonAuthUserRepository nonAuthUserRepository, Queue<Packet> packets, Map<String, Object> stringObjectMap) throws XMPPException {

        // Get sender user ID
        //
        BareJID from = (packet.getStanzaFrom() != null)
                ? packet.getStanzaFrom().getBareJID()
                : null;

        // update user's 'lastRequestAt' field
        //
        if(xmppResourceConnection != null) {
            AuthRepositoryMDImpl authRepository = (AuthRepositoryMDImpl) xmppResourceConnection.getAuthRepository();
            QBAuth qbAuthRepository = (QBAuth) authRepository.getRepo(from.getDomain());
            try {
                qbAuthRepository.updateLastRequestAt(from);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
