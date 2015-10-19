package tigase.stats.qb.processors.message;

import com.quickblox.chat.utils.QBChatUtils;
import tigase.disteventbus.EventBus;
import tigase.server.Packet;
import tigase.stats.qb.processors.ProcessPacket;
import tigase.stats.qb.processors.Util;

/**
 * Created by QuickBlox team on 1/1/15.
 */
public class ProcessVideoCall extends ProcessPacket {
    @Override
    public void execute(Packet packet, String appId, EventBus eventBus) {
        String messageType = packet.getAttributeStaticStr("type");

        try {
            String videoCData = packet.getElement().getChild("extraParams").getChild("signalType").getCData();
            if (messageType.equals("headline")
                    && videoCData.equals("accept")) {
                Util.fireEvent(eventBus, appId, QBChatUtils.ACCEPT_VIDEO_CALL_METRIC);
            }
        } catch (NullPointerException e) {
        }
    }
}
