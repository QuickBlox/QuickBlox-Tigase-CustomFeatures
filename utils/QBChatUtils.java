package com.quickblox.chat.utils;

import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import tigase.xml.CData;
import tigase.xml.Element;
import tigase.xml.XMLNodeIfc;

/**
 * Created by QuickBlox team on 1/1/15.
 */
public class QBChatUtils {

//    private static final String[] decoded = { "&(?!.{2,4};)", "<", ">", "\"", "\'" };
    private static final String[] decoded = { "&(?!amp;|lt;|gt;|quot;|apos;)", "<", ">", "\"", "\'" };
    private static final String[] encoded = { "&amp;", "&lt;", "&gt;", "&quot;", "&apos;" };
    private static final String[] decoded_origin = { "&", "<", ">", "\"", "\'" };

    private static final Logger log = Logger.getLogger(QBChatUtils.class.getName());

    // Supported stats metrics
    //
    public static final String MESSAGE_METRIC = "message";
    public static final String ORDINARY_MESSAGE_METRIC = "ordinaryMessage";
    public static final String PRESENCE_METRIC = "presence";
    public static final String ACCEPT_VIDEO_CALL_METRIC = "acceptVideoCall";
    public static final String COMPOSING_METRIC = "composing";
    public static final String PAUSED_METRIC = "paused";
    public static final String RECEIVED_METRIC = "received";
    public static final String DISPLAYED_METRIC = "displayed";
    public static final String VIDEO_ATTACH_METRIC = "videoAttach";
    public static final String AUDIO_ATTACH_METRIC = "audioAttach";
    public static final String PHOTO_ATTACH_METRIC = "photoAttach";
    public static final String OTHER_ATTACH_METRIC = "otherAttach";
    public static final String CONNECTIONS_METRIC = "connections";
    public static final String UNIQUE_CONNECTIONS_METRIC = "uniqueConnections";
    //Util keys for storing and retrieving statistics
    public static final String APP_ID_KEY = "appId";
    public static final String CREATED_AT_KEY = "createdAt";

    /**
     * Metrics to store besides the "connectionPerUnit" and "uniqueConnections" (custom handled metrics).
     */
    public static final String[] METRICS;

    static {
        String[] metrics;

        try {
            metrics = System.getProperty("QB_CHAT_STATISTICS_METRICS").split(",");
        } catch (SecurityException | NullPointerException | IllegalArgumentException e) {
            log.severe("Exception while parsing metrics for statistics handling: " + e.toString() + ", \nContinuing with an empty METRICS array");
            metrics = new String[0];
        }

        METRICS = metrics;
    }


    public static Integer[] getApplicationIDAndUserIDFromUserJIDLocalPart(String userJIDLocalPart){
        try{
            StringTokenizer jidTokenizer = new StringTokenizer(userJIDLocalPart,"-");
            if(jidTokenizer.countTokens() < 2){
                return null;
            }

            String userIDString = jidTokenizer.nextToken();
            String applicationIDString = jidTokenizer.nextToken();

            Integer userID = null;
            Integer applicationID = null;
            try {
                userID = Integer.parseInt(userIDString);
                applicationID = Integer.parseInt(applicationIDString);
            }catch (NumberFormatException nfe){
                 return null;
            }

            Integer[] result = {applicationID, userID};
            return result;
        }catch(Exception e){
            return null;
        }
    }

    public static boolean isAllowExchangePacketsBetweenJIDs(String JIDLocalPart1, String JIDLocalPart2){
        if(JIDLocalPart1 == null || JIDLocalPart2 == null){
            return true;
        }

        Integer[] result1 = getApplicationIDAndUserIDFromUserJIDLocalPart(JIDLocalPart1);
        Integer[] result2 = getApplicationIDAndUserIDFromUserJIDLocalPart(JIDLocalPart2);

        // ignore if this is a message from a user to component (e.g. muc) or similar
        if(result1 == null || result2 == null){
            return true;
        }

        // Check if this is a packet between users
        // Users must be from same application
        Integer applicationID1 = result1[0];
        Integer applicationID2 = result2[0];

        return applicationID1.equals(applicationID2);
    }

    //
    // XML stuff
    //
    public static String translateAll(String input, String[] patterns, String[] replacements) {
        String result = input;

        for (int i = 0; i < patterns.length; i++) {
            result = result.replaceAll(patterns[i], replacements[i]);
        }

        return result;
    }

    public static String escapeXml(String input) {
        if(input == null){
            return null;
        }
        return translateAll(input, decoded, encoded);
//        return StringEscapeUtils.escapeXml(StringEscapeUtils.unescapeXml(input));
    }

    public static String unescapeXml(String input) {
        if(input == null){
            return null;
        }
        return translateAll(input, encoded, decoded_origin);
    }

    public static void escapeElement(Element element){
        if(element != null) {
            try {
                String originValue = element.getCData();
                if (originValue != null) {
                    String escapedValue = QBChatUtils.escapeXml(originValue);
                    List<XMLNodeIfc> newChildren = new LinkedList<XMLNodeIfc>();
                    newChildren.add(new CData(escapedValue));
                    element.setChildren(newChildren);
                }
            }catch (NullPointerException npe){
                log.severe("NullPointerException while escaping element: " + npe.toString() + ", element: " + element);
                npe.printStackTrace();
            }catch (Exception e){
                log.severe("Exception while escaping element: " + e.toString() + ", element: " + element);
                e.printStackTrace();
            }
        }
    }

    public static void escapeElementBodyAndExtraParams(Element element){
        // element is an instance of packet's element with 'body' and 'extraParams' children

        Element bodyChild = element.getChild("body");
        escapeElement(bodyChild);
        //
        Element extraParamsChild = element.getChild("extraParams");
        if(extraParamsChild != null && extraParamsChild.getChildren() != null){
            for(Element extraParam : extraParamsChild.getChildren()){
                escapeElement(extraParam);
            }
        }
    }

    public static String getIpAddressFromPacketFrom(String packetFrom) throws NullPointerException{
        // example: c2s@ip-10-122-235-20.ec2.internal/10.122.235.20_5222_80.92.234.12_64813

        String[] splitAddresses1 = packetFrom.split("/");
        if(splitAddresses1.length < 2){
            throw new NullPointerException();
        }
        String[] splitAddresses2 = splitAddresses1[1].split("_");
        if(splitAddresses2.length < 4) {
            throw new NullPointerException();
        }
        return splitAddresses2[2];
    }
}
