package tigase.stats.qb.processors.message;

import com.quickblox.chat.utils.QBChatUtils;
import tigase.disteventbus.EventBus;
import tigase.server.Packet;
import tigase.stats.qb.processors.ProcessPacket;
import tigase.stats.qb.processors.Util;

/**
 * Created by QuickBlox team on 1/1/15.
 */
public class ProcessMessage extends ProcessPacket {
    @Override
    public void execute(Packet packet, String appId, EventBus eventBus) {
        Util.fireEvent(eventBus, appId, QBChatUtils.MESSAGE_METRIC);
    }
}
