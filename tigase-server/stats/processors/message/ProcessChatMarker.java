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
public class ProcessChatMarker extends ProcessPacket {
    private static final String CHAT_MARKERS_XMLNS = "urn:xmpp:chat-markers:0";

    @Override
    public void execute(Packet packet, String appId, EventBus eventBus) {
        //received || displayed
        Element packetEl = packet.getElement();
        Element chatMarker = packetEl.getChild(QBChatUtils.RECEIVED_METRIC);

        if (chatMarker == null) {
            chatMarker = packetEl.getChild(QBChatUtils.DISPLAYED_METRIC);
        }
        try {
            if (chatMarker.getAttributeStaticStr("xmlns").equals(CHAT_MARKERS_XMLNS)) {
                Util.fireEvent(eventBus, appId, chatMarker.getName());
            }
        } catch (NullPointerException e) {}
    }
}
