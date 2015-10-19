package com.quickblox.chat.services;

import tigase.muc.Room;
import tigase.xml.Element;
import tigase.xmpp.JID;
import tigase.xmpp.XMPPException;

import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by QuickBlox team on 1/1/15.
 */
public class QBChatServices {

    private static final Logger log = Logger.getLogger(QBChatServices.class.getName());

    // Double Checked Locking & volatile Singleton
    //
    private static volatile QBChatServices instance;
    //
    public static QBChatServices getInstance() {
        QBChatServices localInstance = instance;
        if (localInstance == null) {
            synchronized (QBChatServices.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new QBChatServices();
                }
            }
        }
        return localInstance;
    }


    private MongoClient mongoClient;
    private MongoDatabase db;

    private QBChatServices() {
        // establish MongoDB connection
        String URIString = "...";
        MongoClientURI uri = new MongoClientURI(URIString);
        mongoClient = new MongoClient(uri);

        db = mongoClient.getDatabase(uri.getDatabase());
    }


    //
    // History
    //


    public void savePrivateChatMessageToHistory(Element element, Boolean isOpponentOffline) throws XMPPException {
        //
        // Save to DB here
        //
    }

    public void saveGroupChatMessageToHistory(Element element, Room room) throws XMPPException {
        //
        // Save to DB here
        //
    }


    //
    // Status Messages
    //

    public void saveMessageStatus(Integer from, String readId, String deliveredId) {

        ObjectId readObjectId = null;
        if(readId != null){
            try {
                readObjectId = new ObjectId(readId);
            }catch (IllegalArgumentException e){

            }
        }

        ObjectId deliveredObjectId = null;
        if(deliveredId != null){
            try {
                deliveredObjectId = new ObjectId(deliveredId);
            }catch (IllegalArgumentException e){

            }
        }

        //
        // Save to DB here
        //
    }


    //
    // Statistics
    //

    public void saveChatMessageStaticsPerUnit(List<Map<String, Object>> stats) {
        //
        // Save to DB here
        //
    }
}
