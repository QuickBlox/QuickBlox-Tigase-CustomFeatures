package tigase.stats.qb.processors.message;

import com.quickblox.chat.utils.QBChatUtils;
import tigase.disteventbus.EventBus;
import tigase.server.Packet;
import tigase.stats.qb.processors.ProcessPacket;
import tigase.stats.qb.processors.Util;
import tigase.xml.Element;

/**
 * Created by QuickBlox team on 1/1/15.
 */
public class ProcessChatStateNotification extends ProcessPacket {
    private static final String CHAT_STATE_NOTIFICATIONS_XMLNS = "http://jabber.org/protocol/chatstates";

    @Override
    public void execute(Packet packet, String appId, EventBus eventBus) {
        //composing || paused
        Element packetEl = packet.getElement();
        Element stateNotification = packetEl.getChild(QBChatUtils.COMPOSING_METRIC);

        if (stateNotification == null) {
            stateNotification = packetEl.getChild(QBChatUtils.PAUSED_METRIC);
        }
        try {
            if (stateNotification.getAttributeStaticStr("xmlns").equals(CHAT_STATE_NOTIFICATIONS_XMLNS)) {
                Util.fireEvent(eventBus, appId, stateNotification.getName());
            }
        } catch (NullPointerException e) {
        }
    }
}
