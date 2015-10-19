/*
 * Tigase Jabber/XMPP Multi-User Chat Component
 * Copyright (C) 2008 "Bartosz M. Malkowski" <bartosz.malkowski@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.muc.repository.inmemory;

import com.quickblox.chat.utils.QBChatUtils;
import tigase.component.exceptions.RepositoryException;
import tigase.muc.Affiliation;
import tigase.muc.MucContext;
import tigase.muc.Room;
import tigase.muc.Room.RoomListener;
import tigase.muc.RoomConfig;
import tigase.muc.RoomConfig.RoomConfigListener;
import tigase.muc.exceptions.MUCException;
import tigase.muc.repository.IMucRepository;
import tigase.muc.repository.MucDAO;
import tigase.server.Packet;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by QuickBlox team on 1/1/15.
 */
public class InMemoryMucRepositoryWithSplitRoomsByApps implements IMucRepository {

	private class InternalRoom {
		boolean listPublic = true;
	}

	//
    //
    private final Map<Integer, ConcurrentHashMap<BareJID, InternalRoom>> allRooms = new ConcurrentHashMap<Integer, ConcurrentHashMap<BareJID, InternalRoom> >();


    private final MucDAO dao;

	private RoomConfig defaultConfig;

	protected Logger log = Logger.getLogger(this.getClass().getName());

	private MucContext mucConfig;

	private final RoomConfigListener roomConfigListener;

	private final RoomListener roomListener;

	private final Map<BareJID, Room> rooms = new ConcurrentHashMap<BareJID, Room>();

	public InMemoryMucRepositoryWithSplitRoomsByApps(final MucContext mucConfig, final MucDAO dao) throws RepositoryException {
		this.dao = dao;
		this.mucConfig = mucConfig;

        //
        //
        String[] roomJids = dao.getRoomsJIDList();


        int index = 0;
        if (roomJids != null) {

			int roomsCount = roomJids.length;
			if(log.isLoggable(Level.SEVERE)){
				log.log(Level.SEVERE, "Dividing rooms by applications: " + roomsCount);
			}

            for (String jidString : roomJids) {

                BareJID jid = null;
                try {
                    jid = BareJID.bareJIDInstance(jidString);
                } catch (TigaseStringprepException e) {
                    e.printStackTrace();
                    continue;
                }

                final BareJID owner = this.getRoomOwner(jid);
                if(owner == null){
                    continue;
                }
                final Integer appID = QBChatUtils.getApplicationIDAndUserIDFromUserJIDLocalPart(owner.getLocalpart())[0];

                ConcurrentHashMap<BareJID, InternalRoom> applicationRooms = this.allRooms.get(appID);
                if(applicationRooms == null){
                    applicationRooms = new ConcurrentHashMap<BareJID, InternalRoom>();
                    this.allRooms.put(appID, applicationRooms);
                }
                applicationRooms.put(jid, new InternalRoom());

                if(log.isLoggable(Level.SEVERE)){
                    if(index % 100 == 0){
                        log.log(Level.SEVERE, "Processed rooms: " + index + " from: " + roomsCount);
                    }
                }

                ++index;
            }

            if(log.isLoggable(Level.SEVERE)){
                log.log(Level.SEVERE, "Finished rooms dividing, total processed: " + index);
            }
        }

		this.roomListener = new RoomListener() {

			@Override
			public void onChangeSubject(Room room, String nick, String newSubject, Date changeDate) {
				try {
					if (room.getConfig().isPersistentRoom())
						dao.setSubject(room.getRoomJID(), newSubject, nick, changeDate);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			public void onMessageToOccupants(Room room, JID from, Packet msg) {
				// nothing to do here
			}

			@Override
			public void onSetAffiliation(Room room, BareJID jid, Affiliation newAffiliation) {
				try {
					if (room.getConfig().isPersistentRoom())
						dao.setAffiliation(room.getRoomJID(), jid, newAffiliation);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		};

		this.roomConfigListener = new RoomConfigListener() {

			@Override
			public void onInitialRoomConfig( RoomConfig roomConfig ) {
				try {
					if ( roomConfig.isPersistentRoom() ){
						final Room room = getRoom( roomConfig.getRoomJID() );
						dao.createRoom( room );
					}
				} catch ( Exception e ) {
					throw new RuntimeException( e );
				}
			}

			@Override
			public void onConfigChanged(final RoomConfig roomConfig, final Set<String> modifiedVars) {
				try {
					if (modifiedVars.contains(RoomConfig.MUC_ROOMCONFIG_PUBLICROOM_KEY)) {

						//
                        //
                        final BareJID jid = roomConfig.getRoomJID();
                        final BareJID owner = getRoomOwner(jid);
                        if(owner != null) {
                            final Integer appID = QBChatUtils.getApplicationIDAndUserIDFromUserJIDLocalPart(owner.getLocalpart())[0];
                            final ConcurrentHashMap<BareJID, InternalRoom> applicationRooms = allRooms.get(appID);
                            if (applicationRooms != null) {
                                final InternalRoom ir = applicationRooms.get(jid);
                                if (ir != null) {
                                    ir.listPublic = roomConfig.isRoomconfigPublicroom();
                                }
                            }
                        }
					}

					if (modifiedVars.contains(RoomConfig.MUC_ROOMCONFIG_PERSISTENTROOM_KEY)) {
						if (roomConfig.isPersistentRoom()) {
							final Room room = getRoom(roomConfig.getRoomJID());
							dao.createRoom(room);
						} else {
							dao.destroyRoom(roomConfig.getRoomJID());
						}
					} else if (roomConfig.isPersistentRoom()) {
						dao.updateRoomConfig(roomConfig);
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		};
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tigase.muc.repository.IMucRepository#createNewRoom(java.lang.String,
	 * java.lang.String, java.lang.String)
	 */
	@Override
	public Room createNewRoom(BareJID roomJID, JID senderJid) throws RepositoryException {
		if (log.isLoggable(Level.FINE))
			log.fine("Creating new room '" + roomJID + "'");

		RoomConfig rc = new RoomConfig(roomJID, this.mucConfig.isPublicLoggingEnabled());

		rc.copyFrom(getDefaultRoomConfig(), false);

		Room room = Room.newInstance(rc, new Date(), senderJid.getBareJID());
		room.getConfig().addListener(roomConfigListener);
		room.addListener(roomListener);
		this.rooms.put(roomJID, room);


        //
        //
        final Integer appID = QBChatUtils.getApplicationIDAndUserIDFromUserJIDLocalPart(senderJid.getLocalpart())[0];
        ConcurrentHashMap<BareJID, InternalRoom> applicationRooms = allRooms.get(appID);
        if(applicationRooms == null){
            applicationRooms = new ConcurrentHashMap<BareJID, InternalRoom>();
            allRooms.put(appID, applicationRooms);
        }
        applicationRooms.put(roomJID, new InternalRoom());

		return room;
	}

    //
    //
	@Override
	public void destroyRoom(Room room, Element destroyElement) throws RepositoryException {
        final BareJID roomJID = room.getRoomJID();
        destroyRoom(roomJID, destroyElement);
	}
    @Override
    public void destroyRoom(BareJID roomJID, Element destroyElement) throws RepositoryException {
        if (log.isLoggable(Level.FINE)) {
            log.fine("Destroying room '" + roomJID);
        }
        this.rooms.remove(roomJID);

		//
		//
        final BareJID owner = getRoomOwner(roomJID);
        if(owner != null) {
            final Integer appID = QBChatUtils.getApplicationIDAndUserIDFromUserJIDLocalPart(owner.getLocalpart())[0];
            ConcurrentHashMap<BareJID, InternalRoom> applicationRooms = allRooms.get(appID);
            if (applicationRooms != null) {
                applicationRooms.remove(roomJID);
            }
        }

        dao.destroyRoom(roomJID);
    }

	@Override
	public Map<BareJID, Room> getActiveRooms() {
		return Collections.unmodifiableMap(rooms);
	}

	@Override
	public RoomConfig getDefaultRoomConfig() throws RepositoryException {
		if (defaultConfig == null) {
			defaultConfig = new RoomConfig(null, this.mucConfig.isPublicLoggingEnabled());
			try {
				defaultConfig.read(dao.getRepository(), mucConfig, MucDAO.ROOMS_KEY + null + "/config");
			} catch (Exception e) {
				e.printStackTrace();
			}
			// dao.updateRoomConfig(defaultConfig);
		}
		return defaultConfig;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tigase.muc.repository.IMucRepository#getRoomsIdList()
	 */
    //
    //
	@Override
	public BareJID[] getPublicVisibleRoomsIdList() throws RepositoryException {
        throw new RepositoryException("Not supported");
	}
    //
    @Override
    public BareJID[] getPublicVisibleRoomsIdList(Integer appID) throws RepositoryException{
        List<BareJID> result = new ArrayList<BareJID>();

        // get rooms for particular application
        ConcurrentHashMap<BareJID, InternalRoom> applicationRooms = this.allRooms.get(appID);
        if(applicationRooms == null){
            applicationRooms = new ConcurrentHashMap<BareJID, InternalRoom>();
        }

        for (Entry<BareJID, InternalRoom> entry : applicationRooms.entrySet()) {
            if (entry.getValue().listPublic) {
                result.add(entry.getKey());
            }
        }

        return result.toArray(new BareJID[] {});
    }

	/*
	 * (non-Javadoc)
	 * 
	 * @see tigase.muc.repository.IMucRepository#getRoom()
	 */
	@Override
	public Room getRoom(final BareJID roomJID) throws RepositoryException, MUCException {
		Room room = this.rooms.get(roomJID);
		if (room == null) {
			room = dao.readRoom(roomJID);
			if (room != null) {
				room.getConfig().addListener(roomConfigListener);
				room.addListener(roomListener);
				this.rooms.put(roomJID, room);
			}
		}
		return room;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tigase.muc.repository.IMucRepository#getRoomName(java.lang.String)
	 */
	@Override
	public String getRoomName(String jid) throws RepositoryException {
		Room r = rooms.get(BareJID.bareJIDInstanceNS(jid));
		if (r != null) {
			return r.getConfig().getRoomName();
		} else {
			return dao.getRoomName(jid);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * tigase.muc.repository.IMucRepository#isRoomIdExists(java.lang.String)
	 */
	@Override
	public boolean isRoomIdExists(String newRoomName) {

        //
        //
        for(Entry<Integer, ConcurrentHashMap<BareJID, InternalRoom> >  applicationRooms : this.allRooms.entrySet()){
            final ConcurrentHashMap<BareJID, InternalRoom> room = applicationRooms.getValue();
            if (room.containsKey(newRoomName)){
                return true;
            }
        }
        return false;
	}

	@Override
	public void leaveRoom(Room room) {
		final BareJID roomJID = room.getRoomJID();
		if (log.isLoggable(Level.FINE))
			log.fine("Removing room '" + roomJID + "' from memory");
		this.rooms.remove(roomJID);

		if (!room.getConfig().isPersistentRoom()) {

            //
            //
            BareJID owner = null;
            try {
                owner = getRoomOwner(roomJID);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            if(owner != null) {
                final Integer appID = QBChatUtils.getApplicationIDAndUserIDFromUserJIDLocalPart(owner.getLocalpart())[0];
                ConcurrentHashMap<BareJID, InternalRoom> applicationRoom = allRooms.get(appID);
                if (applicationRoom != null) {
                    applicationRoom.remove(roomJID);
                }
            }
		}
	}

	@Override
	public void updateDefaultRoomConfig(RoomConfig config) throws RepositoryException {
		RoomConfig org = getDefaultRoomConfig();
		org.copyFrom(config);
		dao.updateRoomConfig(defaultConfig);
	}

    //
    //
    public BareJID getRoomOwner(BareJID roomJid) throws RepositoryException {
        try {
            Room room = this.rooms.get(BareJID.bareJIDInstance(roomJid.toString()));
            if (room == null) {
                // Hmm...check in database
                String owner = dao.getRoomOwner(roomJid.toString());
                return owner == null ? null : BareJID.bareJIDInstance(owner);
            }
            return room.getCreatorJid();
        } catch (Exception e) {
            if (log.isLoggable(Level.FINE)) {
                log.fine("Exception: " + e.toString());
            }
            return null;
        }
    }
}
