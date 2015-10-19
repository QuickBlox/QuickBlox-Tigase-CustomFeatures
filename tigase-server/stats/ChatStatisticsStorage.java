package tigase.stats.qb;

import com.quickblox.chat.utils.QBChatUtils;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by QuickBlox team on 1/1/15.
 */
public enum ChatStatisticsStorage {
    INSTANCE;

    private Map<String, FrequencyStanza> stanzaConcurrentHashMap = new ConcurrentHashMap<>();

    /**
     * appId comes in <userId>-<appId> or just <appId> formats.
     *
     * @param appId
     * @param userId
     * @param property
     */
    public void increment(String appId, String userId, String property) {
        if (!stanzaConcurrentHashMap.containsKey(appId)) {
            createFrequencyStanza(appId);
        }
        stanzaConcurrentHashMap.get(appId).increment(userId, property);
    }

    /**
     * @param appId
     * @param userId
     * @param property
     */
    public void decrement(String appId, String userId, String property) {
        stanzaConcurrentHashMap.get(appId).decrement(userId, property);
    }

    public Set<String> getAppIds() {
        return stanzaConcurrentHashMap.keySet();
    }

    public Long getPropertyByAppId(String appId, String property) {
        return stanzaConcurrentHashMap.get(appId).getProperty(property);
    }

    public void clearPropertyByAppId(String appId, String property) {
        stanzaConcurrentHashMap.get(appId).clearProperty(property);
    }

    private void createFrequencyStanza(String appId) {
        stanzaConcurrentHashMap.put(appId, new FrequencyStanza());
    }



    private class FrequencyStanza {
        private ConcurrentHashMap<String, Integer> connectionsStats = new ConcurrentHashMap<>();
        /**
         * Container for all the other metrics besides the "connectionPerUnit" and "uniqueConnections" number.
         */
        private ConcurrentHashMap<String, Long> otherMetricsStats = new ConcurrentHashMap<>();
        {
            for (int i = 0; i < QBChatUtils.METRICS.length; i++) {
                otherMetricsStats.put(QBChatUtils.METRICS[i], new Long(0));
            }
        }

        public Long getProperty(String property) {
            Long temp = null;

            if (property.equals(QBChatUtils.CONNECTIONS_METRIC)) {
                temp = this.countConnections();
            } else if (property.equals(QBChatUtils.UNIQUE_CONNECTIONS_METRIC)) {
                temp = (long) this.connectionsStats.size();
            } else {
                temp = otherMetricsStats.get(property);
            }

            return temp;
        }

        /**
         * Counts the overall number of active connectionsStats.
         *
         * @return
         */
        private Long countConnections() {
            long c = 0l;
            for (Integer userConnection : connectionsStats.values()) {
                c += userConnection.intValue();
            }
            return c;
        }

        public void increment(String userId, String property) {
            if (property.equals(QBChatUtils.CONNECTIONS_METRIC)) {
                this.incrementConnections(userId);
            } else {
                otherMetricsStats.replace(property, otherMetricsStats.get(property) + 1);
            }
        }

        private void incrementConnections(String userId) {
            if (!connectionsStats.containsKey(userId)) {
                connectionsStats.put(userId, 1);
            } else {
                connectionsStats.replace(userId, connectionsStats.get(userId) + 1);
            }
        }

        public void decrement(String userId, String property) {
            if (property.equals(QBChatUtils.CONNECTIONS_METRIC)) {
                this.decrementConnections(userId);
            } else {
                otherMetricsStats.replace(property, otherMetricsStats.get(property) - 1);
            }
        }

        private void decrementConnections(String userId) {
            connectionsStats.replace(userId, connectionsStats.get(userId) - 1);
            Integer userConnections = connectionsStats.get(userId);
            if (userConnections <= 0) {
                connectionsStats.remove(userId, userConnections);
            }
        }

        public void clearProperty(String property) {
            if (property.equals(QBChatUtils.CONNECTIONS_METRIC)) {
                this.connectionsStats = new ConcurrentHashMap<>();
            } else {
                otherMetricsStats.replace(property, new Long(0));
            }
        }
    }
}