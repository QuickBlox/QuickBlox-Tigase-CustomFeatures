package tigase.stats.qb;



import com.quickblox.chat.utils.QBChatUtils;
import com.quickblox.chat.services.QBChatServices;
import tigase.disteventbus.EventBus;
import tigase.disteventbus.EventBusFactory;
import tigase.disteventbus.EventHandler;
import tigase.stats.StatisticsArchivizerIfc;
import tigase.stats.StatisticsProvider;
import tigase.xml.Element;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by QuickBlox team on 1/1/15.
 */
public class ChatStatisticsPerUnit implements StatisticsArchivizerIfc, EventHandler {

    /**
     * The name of the incoming event element for {@link EventBus}
     */
    public static final String EVENT_NAME = "stats_metric_event";

    private EventBus eventBus;
    private ChatStatisticsStorage chatStatisticsStorage;
    private QBChatServices chatServices;
    private Double timeout;

    private static final Logger log = Logger.getLogger(ChatStatisticsPerUnit.class.getName());

    @Override
    public void execute(StatisticsProvider sp) {

        // Dump data to DB
        //
        List<Map<String, Object>> statisticsForApps = new LinkedList<>();
        for (String appId : chatStatisticsStorage.getAppIds()) {

            HashMap<String, Object> statsEntry = new HashMap<>();
            statsEntry.put(QBChatUtils.APP_ID_KEY, appId);
            statsEntry.put(QBChatUtils.CONNECTIONS_METRIC, chatStatisticsStorage.getPropertyByAppId(appId, QBChatUtils.CONNECTIONS_METRIC));
            statsEntry.put(QBChatUtils.UNIQUE_CONNECTIONS_METRIC, chatStatisticsStorage.getPropertyByAppId(appId, QBChatUtils.UNIQUE_CONNECTIONS_METRIC));
            for (int i = 0; i < QBChatUtils.METRICS.length; i++) {
                statsEntry.put(QBChatUtils.METRICS[i], chatStatisticsStorage.getPropertyByAppId(appId, QBChatUtils.METRICS[i]) / timeout);
            }

            statisticsForApps.add(statsEntry);

            for (int i = 0; i < QBChatUtils.METRICS.length; i++) {
                chatStatisticsStorage.clearPropertyByAppId(appId, QBChatUtils.METRICS[i]);
            }
        }
        chatServices.saveChatMessageStaticsPerUnit(statisticsForApps);
    }

    @Override
    public void init(Map<String, Object> archivizerConf) {
        eventBus = EventBusFactory.getInstance();
        chatServices = QBChatServices.getInstance();
        chatStatisticsStorage = ChatStatisticsStorage.INSTANCE;
        timeout = (archivizerConf.get("timeout") != null ? Double.valueOf((String) archivizerConf.get("timeout")) : 5.0);

        eventBus.addHandler(ChatStatisticsPerUnit.EVENT_NAME, "jabber:client", this);
    }

    @Override
    public void release() {
    }

    @Override
    public void onEvent(String name, String xmlns, Element event) {
        String appId = event.getAttributeStaticStr(QBChatUtils.APP_ID_KEY);
        String userId = event.getAttributeStaticStr("user_id");
        String metric = event.getAttributeStaticStr("metric");
        // action-attribute indicates that the element is connection
        String action = event.getAttributeStaticStr("action");

        //in case appId is invalid -> log incoming event elemt
        try {
            Integer.valueOf(appId);
        } catch (NumberFormatException e) {
            log.warning("Bad appId format : event type = " + name + "; event = " + event);
            return;
        }
        if (action == null || action.equals("increment")) {
            chatStatisticsStorage.increment(appId, userId, metric);
        } else {
            chatStatisticsStorage.decrement(appId, userId, metric);
        }
    }
}
