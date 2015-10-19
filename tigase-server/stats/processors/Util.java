package tigase.stats.qb.processors;

import com.quickblox.chat.utils.QBChatUtils;
import tigase.disteventbus.EventBus;
import tigase.server.Packet;
import tigase.stats.qb.ChatStatisticsPerUnit;
import tigase.xml.Element;
import tigase.xmpp.impl.QBStatsCollector;

/**
 * Helper class to fire an event on command.
 */
/**
 * Created by QuickBlox team on 1/1/15.
 */
public class Util {
    public static void fireEvent(EventBus eventBus, String appId, String metric) {
        eventBus.fire(new Element(ChatStatisticsPerUnit.EVENT_NAME, (String) null,
                new String[]{Packet.XMLNS_ATT, QBChatUtils.APP_ID_KEY, "metric"},
                new String[]{QBStatsCollector.CLIENT_XMLNS, appId, metric}));
    }
}
