package main.java.com.quickblox.chat.customobjectsplugin.model;

import main.java.com.quickblox.chat.customobjectsplugin.util.TextUtil;
import org.apache.commons.lang.StringEscapeUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by QuickBlox team on 1/1/15.
 */
public class Message {
    public static final String EQUALS = "=";
    public static final String AMPERSAND = "&";

    private static final StringBuilder sb = new StringBuilder();

    private String receiverIdFieldName;
    private String dateSentFieldName;
    private String messageFieldName;

    private String receiverId;
    private String message;
    private String additional;
    private long dateSent;

    public Message(String receiverId, String message, String additional) throws UnsupportedEncodingException {
        if (!TextUtil.isEmpty(receiverId)) {
            this.receiverId = URLEncoder.encode(receiverId, TextUtil.UTF8);
        }
        if (!TextUtil.isEmpty(message)) {
            // XML unescape
            String unescapeMessage = StringEscapeUtils.unescapeXml(message);

            // URL encode
            String encodedMessage = URLEncoder.encode(unescapeMessage, TextUtil.UTF8);

            this.message = encodedMessage;
        }
        this.additional = additional;
        this.dateSent = System.currentTimeMillis();
    }

    public void configureDefaultFieldsNames(String receiverIDFieldName, String dateSentFieldName, String messageFieldName){
        this.receiverIdFieldName = receiverIDFieldName;
        this.dateSentFieldName = dateSentFieldName;
        this.messageFieldName = messageFieldName;
    }

    public String getRecepientId() {
        return receiverId;
    }

    public void setRecepientId(String recepientId) {
        this.receiverId = recepientId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAdditional() {
        return additional;
    }

    public void setAdditional(String additional) {
        this.additional = additional;
    }

    @Override
    public synchronized String toString() {
        sb.setLength(0);

        // add 'receiver_id' field
        if(this.receiverIdFieldName != null){
            sb.append(this.receiverIdFieldName).append(EQUALS).append(receiverId);
        }

        // add 'dateSent' field
        if(this.dateSentFieldName != null){
            sb.append(AMPERSAND).append(this.dateSentFieldName).append(EQUALS).append(dateSent);
        }

        // add 'message' field
        if(this.messageFieldName != null){
            sb.append(AMPERSAND).append(this.messageFieldName).append(EQUALS).append(message);
        }

        // add additional fields
        if (!TextUtil.isEmpty(additional)) {
            sb.append(AMPERSAND).append(additional);
        }

        return sb.toString();
    }
}
