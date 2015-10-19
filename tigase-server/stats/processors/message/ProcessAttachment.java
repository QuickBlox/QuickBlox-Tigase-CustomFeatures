package tigase.stats.qb.processors.message;

import com.quickblox.chat.utils.QBChatUtils;
import tigase.disteventbus.EventBus;
import tigase.server.Packet;
import tigase.stats.qb.processors.ProcessPacket;
import tigase.stats.qb.processors.Util;
import tigase.xml.Element;

import java.util.List;

/**
 * Created by QuickBlox team on 1/1/15.
 */
public class ProcessAttachment extends ProcessPacket {
    @Override
    public void execute(Packet packet, String appId, EventBus eventBus) {
        try {
            List<Element> children;
            String attachType;

            try {
                children = packet.getElement().getChild("extraParams").getChildren();
            } catch (NullPointerException e) {
                return;
            }

            for (Element child : children) {
                if (child.getName().equals("attachment")) {
                    attachType = child.getAttributeStaticStr("type");
                    switch (attachType) {
                        case "video":
                            Util.fireEvent(eventBus, appId, QBChatUtils.VIDEO_ATTACH_METRIC);
                            break;
                        case "audio":
                            Util.fireEvent(eventBus, appId, QBChatUtils.AUDIO_ATTACH_METRIC);
                            break;
                        case "photo":
                            Util.fireEvent(eventBus, appId, QBChatUtils.PHOTO_ATTACH_METRIC);
                            break;
                        default:
                            Util.fireEvent(eventBus, appId, QBChatUtils.OTHER_ATTACH_METRIC);
                            break;
                    }
                }
            }
        } catch (NullPointerException e) {
        }
    }
}
