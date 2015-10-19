package main.java.com.quickblox.chat.customobjectsplugin;

import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;
import tigase.server.Packet;
import tigase.xmpp.JID;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import main.java.com.quickblox.chat.customobjectsplugin.model.Message;
import main.java.com.quickblox.chat.customobjectsplugin.util.TextUtil;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by QuickBlox team on 1/1/15.
 */
public class MessageToCustomObject extends XMPPProcessor implements XMPPProcessorIfc {

    public static final String ID = "messagetocustomobject";

    public static final String CONFIG_QB_DOMAIN_URL = "QB_DOMAIN";
    public static final String CONFIG_QB_CLASS_RECEIVER_ID_FIELD_NAME = "CLASS_RECEIVER_ID_FIELD_NAME";
    public static final String CONFIG_QB_CLASS_DATE_SENT_FIELD_NAME = "CLASS_DATE_SENT_FIELD_NAME";
    public static final String CONFIG_QB_CLASS_MESSAGE_FIELD_NAME = "CLASS_MESSAGE_FIELD_NAME";

    public static final String DEFAULT_CLASS_NAME = "ChatMessage";

    public static final String BODY = "/message/body";
    public static final String TOKEN = "/message/quickblox/token";
    public static final String CLASS_NAME = "/message/quickblox/class_name";
    public static final String ADDITIONAL_FIELDS = "/message/quickblox/additional";

    private static final int MAX_THREAD_COUNT = 4;

    private static final Logger log = Logger.getLogger(MessageToCustomObject.class.getName());

    private BlockingQueue<Runnable> taskQueue = null;
    private List<PoolThread> threadList = new ArrayList<PoolThread>();

    private String domainUrl;
    private String classReceiverIDFieldName;
    private String classDateSentFieldName;
    private String classMessageFieldName;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String[] supElements() {
        return new String[]{"message"};
    }

    @Override
    public String[] supNamespaces() {
        return new String[]{"jabber:client"};
    }

    @Override
    public void init(Map<String, Object> settings) throws TigaseDBException {
        super.init(settings);

        // get data from config
        domainUrl = (String) settings.get(CONFIG_QB_DOMAIN_URL);
        classReceiverIDFieldName = (String) settings.get(CONFIG_QB_CLASS_RECEIVER_ID_FIELD_NAME);
        classDateSentFieldName = (String) settings.get(CONFIG_QB_CLASS_DATE_SENT_FIELD_NAME);
        classMessageFieldName = (String) settings.get(CONFIG_QB_CLASS_MESSAGE_FIELD_NAME);

        taskQueue = new ArrayBlockingQueue<Runnable>(MAX_THREAD_COUNT);

        for (int i = 0; i < MAX_THREAD_COUNT; i++) {
            threadList.add(new PoolThread(taskQueue));
        }

        for (int i = 0; i < MAX_THREAD_COUNT; i++) {
            threadList.get(i).start();
        }
    }

    @Override
    public void process(Packet packet, XMPPResourceConnection session,
            NonAuthUserRepository repo, Queue<Packet> results,
            Map<String, Object> settings) throws XMPPException {

        if (session == null) {
            return;
        }

        JID jid = packet.getFrom();
        if (!session.getConnectionId().equals(jid)) {
            return;
        }

        String token = packet.getElemCData(TOKEN);
        if (TextUtil.isEmpty(token)) {
            return;
        }

        // Log packet
        logPacket(packet);

        // Extract message parameters
        //
        String className = packet.getElemCData(CLASS_NAME);
        if (TextUtil.isEmpty(className)) {
            className = DEFAULT_CLASS_NAME;
        }
        String additional = packet.getElemCData(ADDITIONAL_FIELDS);
        if(additional != null){
            additional = additional.replace("&amp;", "&");
        }
        String body = packet.getElemCData(BODY);
        String receiverId = packet.getStanzaTo().getLocalpart().split("-")[0];

        try {
            // Create message
            Message message = new Message(receiverId, body, additional);

            // configure fields
            message.configureDefaultFieldsNames(classReceiverIDFieldName, classDateSentFieldName, classMessageFieldName);

            // log message
            logMessage(token, className, message);

            // Post message to QuickBlox Custom Objects
            //
            CustomObjectPostSender customObjectSender = new CustomObjectPostSender(domainUrl, token, className, message);
            taskQueue.put(customObjectSender);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void logPacket(Packet packet) {
        if (packet == null) {
            return;
        }
        if (!log.isLoggable(Level.FINEST)) {
            return;
        }

        JID from = packet.getFrom();
        JID stanzaTo = packet.getStanzaTo();

        log.log(Level.FINEST, "PACKET Type " + packet.getType().name() +
                ". Sender JID " + from.toString() +
                ". Receiver JID " + stanzaTo.toString()
                + ". Body " + packet.getElemCData(BODY));
    }

    private void logMessage(String qbtoken, String className, Message message) {
        if (log.isLoggable(Level.FINEST)) {
            log.log(Level.FINEST, "QB Token: " + qbtoken +
                    ". Class: " + className +
                    ". Message: " + message.toString());
        }
    }
}
