/*
 * MessageAmp.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 */



package tigase.xmpp.impl;

//~--- non-JDK imports --------------------------------------------------------

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.quickblox.chat.services.QBChatServices;
import com.quickblox.chat.utils.QBChatUtils;
import tigase.db.MsgRepositoryIfc;
import tigase.db.NonAuthUserRepository;
import tigase.db.RepositoryFactory;
import tigase.db.TigaseDBException;
import tigase.db.UserNotFoundException;
import tigase.server.Packet;
import tigase.server.amp.AmpFeatureIfc;
import tigase.server.amp.MsgRepository;
import tigase.util.DNSResolver;
import tigase.xml.Element;
import tigase.xml.CData;
import tigase.xml.XMLNodeIfc;
import tigase.xmpp.*;
import tigase.xmpp.JID;


/**
 * Created: Apr 29, 2010 5:00:25 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class MessageAmp
				extends XMPPProcessor
				implements XMPPPacketFilterIfc, XMPPPostprocessorIfc, 
						XMPPPreprocessorIfc, XMPPProcessorIfc {
	private static final String     AMP_JID_PROP_KEY     = "amp-jid";
	private static final String[][] ELEMENTS             = {
		{ "message" }, { "presence" }
	};
	private static final String     FROM_CONN_ID         = "from-conn-id";
	private static final String     ID                   = "amp";
	private static final Logger     log = Logger.getLogger(MessageAmp.class.getName());
	private static final String     MSG_OFFLINE_PROP_KEY = "msg-offline";
	private static final String     OFFLINE              = "offline";
	private static final String     TO_CONN_ID           = "to-conn-id";
	private static final String     TO_RES               = "to-res";
	private static final String     XMLNS                = "http://jabber.org/protocol/amp";
	private static final String[]   XMLNSS = { "jabber:client", "jabber:client" };
	private static Element[]        DISCO_FEATURES = { new Element("feature",
			new String[] { "var" }, new String[] { XMLNS }),
			new Element("feature", new String[] { "var" }, new String[] { "msgoffline" }) };
	private static final String defHost = DNSResolver.getDefaultHostname();

	//~--- fields ---------------------------------------------------------------

	private JID             ampJID           = null;
	private MsgRepositoryIfc   msg_repo         = null;
	private OfflineMessages offlineProcessor = new OfflineMessages();
	private Message         messageProcessor = new Message();

	//~--- methods --------------------------------------------------------------

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void init(Map<String, Object> settings) throws TigaseDBException {
		super.init(settings);

		String ampJIDstr = (String) settings.get(AMP_JID_PROP_KEY);

		if (null != ampJIDstr) {
			ampJID = JID.jidInstanceNS(ampJIDstr);
		} else {
			ampJID = JID.jidInstanceNS("amp@" + defHost);
		}
		log.log(Level.CONFIG, "Loaded AMP_JID option: {0} = {1}", new Object[] {
				AMP_JID_PROP_KEY,
				ampJID });

		String off_val = (String) settings.get(MSG_OFFLINE_PROP_KEY);

		if (off_val == null) {
			off_val = System.getProperty(MSG_OFFLINE_PROP_KEY);
		}
		if ((off_val != null) &&!Boolean.parseBoolean(off_val)) {
			log.log(Level.CONFIG, "Offline messages storage: {0}", new Object[] { off_val });
			offlineProcessor = null;
			DISCO_FEATURES = new Element[] { new Element("feature", new String[] { "var" },
					new String[] { XMLNS }) };
		}

		String msg_repo_uri = (String) settings.get(AmpFeatureIfc.AMP_MSG_REPO_URI_PROP_KEY);
		String msg_repo_cls = (String) settings.get(AmpFeatureIfc.AMP_MSG_REPO_CLASS_PROP_KEY);
		
		if (msg_repo_uri == null) {
			msg_repo_uri = System.getProperty(AmpFeatureIfc.AMP_MSG_REPO_URI_PROP_KEY);
			if (msg_repo_uri == null) {
				msg_repo_uri = System.getProperty(RepositoryFactory.GEN_USER_DB_URI_PROP_KEY);
			}
		}
		if (msg_repo_cls == null) {
			msg_repo_cls = System.getProperty(AmpFeatureIfc.AMP_MSG_REPO_CLASS_PROP_KEY);
		}
		if (msg_repo_uri != null) {
			Map<String, String> db_props = new HashMap<String, String>(4);

			for (Map.Entry<String, Object> entry : settings.entrySet()) {
				db_props.put(entry.getKey(), entry.getValue().toString());
			}

			// Initialization of repository can be done here and in Store
			// class so repository related parameters for JDBCMsgRepository
			// should be specified for AMP plugin and AMP component
			try {
				msg_repo = MsgRepository.getInstance(msg_repo_cls, msg_repo_uri);
				msg_repo.initRepository(msg_repo_uri, db_props);
			} catch (TigaseDBException ex) {
				msg_repo = null;
				log.log(Level.WARNING, "Problem initializing connection to DB: ", ex);
			}
		}
	}

	@Override
	public void filter(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo, Queue<Packet> results) {
		C2SDeliveryErrorProcessor.filter(packet, session, repo, results, ampJID);
	}
	
	@Override
	public void postProcess(Packet packet, XMPPResourceConnection session,
			NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings) {
		if ((offlineProcessor != null) && (session == null)) {
			if (packet.getElemName() == tigase.server.Message.ELEM_NAME 
					&& packet.getStanzaTo() != null && packet.getStanzaTo().getResource() != null) {
				return;
			}
			
			Element amp = packet.getElement().getChild("amp");

			if ((amp == null) || (amp.getXMLNS() != XMLNS) || (amp.getAttributeStaticStr(
					"status") != null)) {
				try {
                    //
                    //
					offlineProcessor.savePacketForOffLineUser(packet, msg_repo);
				} catch (UserNotFoundException ex) {
					if (log.isLoggable(Level.FINEST)) {
						log.finest(
								"UserNotFoundException at trying to save packet for off-line user." +
								packet);
					}
				}
			}
		}
	}

	@Override
	public boolean preProcess(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings) {
		boolean result = C2SDeliveryErrorProcessor.preProcess(packet, session, repo, results, settings);
		if (result && packet.getPacketFrom() != null && packet.getPacketFrom().equals(ampJID)) {
			result = false;
		}
		if (result) {
			packet.processedBy(ID);
		}

        //
        //
        if(!result){
            if(packet.getElemName() == tigase.server.Message.ELEM_NAME){
                // QB:  logic to prevent sending messages between different applications
                //
                JID stanzaTo = packet.getStanzaTo();
                JID stanzaFrom = packet.getStanzaFrom();

                if(stanzaTo != null && stanzaFrom != null) {
                    if (!QBChatUtils.isAllowExchangePacketsBetweenJIDs(stanzaTo.getLocalpart(), stanzaFrom.getLocalpart())) {
                        if (log.isLoggable(Level.SEVERE)) {
                            log.log(Level.SEVERE, "FORBIDDEN to exchange packets between JIDs. FROM: {0} TO: {1}",
                                    new Object[]{stanzaFrom, stanzaTo});
                        }

                        return true;
                    }


                    // Replace bad XML characters on sender's side
                    //
                    try {
                        if (session != null && session.isUserId(stanzaFrom.getBareJID())) {
                            // Escaping fix: Escape invalid XML characters in 'body'
                            //
                            QBChatUtils.escapeElementBodyAndExtraParams(packet.getElement());
                            //
                            //
                        }
                    }catch (NotAuthorizedException e){

                    }
                }
            }
        }
        //
        //

		return result;
	}
	
	@Override
	public void process(Packet packet, XMPPResourceConnection session,
			NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings)
					throws XMPPException {

		if (packet.getElemName() == "presence") {
			if ((offlineProcessor != null) && offlineProcessor.loadOfflineMessages(packet,
					session)) {
				try {
					Queue<Packet> packets = offlineProcessor.restorePacketForOffLineUser(session,
							msg_repo);

					if (packets != null) {
						if (log.isLoggable(Level.FINER)) {
							log.finer("Sending off-line messages: " + packets.size());
						}
						results.addAll(packets);
					}    // end of if (packets != null)
				} catch (UserNotFoundException e) {
					log.info("Something wrong, DB problem, cannot load offline messages. " + e);
				}      // end of try-catch
				
				// notify AMP component that user is online now
				if (packet.getStanzaTo() == null) {
					Packet notification = packet.copyElementOnly();
					notification.initVars(session.getJID(), ampJID);
					results.offer(notification);
				}
			}
		} else {
			Element amp = packet.getElement().getChild("amp", XMLNS);

			if ((amp == null) || (amp.getAttributeStaticStr("status") != null)) {
				messageProcessor.process(packet, session, repo, results, settings);
			} else {
				Packet result = packet.copyElementOnly();

				result.setPacketTo(ampJID);
				results.offer(result);
				if (session == null) {
					result.getElement().addAttribute(OFFLINE, "1");

					return;
				}
				if (session.isUserId(packet.getStanzaTo().getBareJID())) {
					result.getElement().addAttribute(TO_CONN_ID, session.getConnectionId()
							.toString());
					result.getElement().addAttribute(TO_RES, session.getResource());
				} else {
					JID connectionId = session.getConnectionId();

					if (connectionId.equals(packet.getPacketFrom())) {
						result.getElement().addAttribute(FROM_CONN_ID, connectionId.toString());
					}
				}
			}
		}
	}

	@Override
	public Element[] supDiscoFeatures(final XMPPResourceConnection session) {
		return DISCO_FEATURES;
	}

	@Override
	public String[][] supElementNamePaths() {
		return ELEMENTS;
	}

	@Override
	public String[] supNamespaces() {
		return XMLNSS;
	}
}
