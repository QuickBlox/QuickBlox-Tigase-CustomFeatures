/*
 * QuickBlox team, Igor Khomenko
 */

package tigase.db.jdbc;

import com.quickblox.chat.utils.QBChatUtils;
import tigase.db.AuthRepository;
import tigase.db.AuthorizationException;
import tigase.db.DBInitException;
import tigase.db.DataRepository;
import tigase.db.RepositoryFactory;
import tigase.db.TigaseDBException;
import tigase.db.UserExistsException;
import tigase.db.UserNotFoundException;
import tigase.xmpp.BareJID;

import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by QuickBlox team on 1/1/15.
 */
public class QBAuth implements AuthRepository {

    /**
     * Private logger for class instances.
     */
    private static final Logger log = Logger.getLogger(TigaseCustomAuth.class.getName());

    /**
     * Retrieves user password from the database for given user_id (JID).
     *
     * Takes 1 argument: <code>(user_id (JID))</code>
     *
     * Example query:
     *
     * <pre>
     * select user_pw from tig_users where user_id = ?
     * </pre>
     */
    public static final String DEF_GETPASSWORD_KEY = "get-password-query";
    public static final String DEF_UPDATE_LAST_REQUEST_AT_KEY = "update_last_request_at-query";

    /**
     * Comma separated list of NON-SASL authentication mechanisms. Possible
     * mechanisms are: <code>password</code> and <code>digest</code>.
     * <code>digest</code> mechanism can work only with
     * <code>get-password-query</code> active and only when password are stored in
     * plain text format in the database.
     */
    public static final String DEF_NONSASL_MECHS_KEY = "non-sasl-mechs";


    /**
     * Comma separated list of SASL authentication mechanisms. Possible mechanisms
     * are all mechanisms supported by Java implementation. The most common are:
     * <code>PLAIN</code>, <code>DIGEST-MD5</code>, <code>CRAM-MD5</code>.
     *
     * "Non-PLAIN" mechanisms will work only with the
     * <code>get-password-query</code> active and only when passwords are stored
     * in plain text format in the database.
     */
    public static final String DEF_SASL_MECHS_KEY = "sasl-mechs";

    public static final String NO_QUERY = "none";

    /** Field description */
    public static final String DEF_INITDB_QUERY = "{ call TigInitdb() }";

    /** Field description */
    public static final String DEF_ADDUSER_QUERY = "{ call TigAddUserPlainPw(?, ?) }";

    /** Field description */
    public static final String DEF_GETPASSWORD_QUERY = "SELECT ...";

    public static final String UPDATE_LAST_REQUEST_AT_QUERY = "UPDATE ...";

    /** Field description */
    public static final String DEF_NONSASL_MECHS = "password";

    /** Field description */
    public static final String DEF_SASL_MECHS = "PLAIN";



    // ~--- fields ---------------------------------------------------------------
    private DataRepository data_repo = null;
    private String initdb_query = DEF_INITDB_QUERY;
    private String getpassword_query = DEF_GETPASSWORD_QUERY;
    private String updateLastRequestAt_query = UPDATE_LAST_REQUEST_AT_QUERY;
    private String adduser_query = DEF_ADDUSER_QUERY;

    private String[] sasl_mechs = DEF_SASL_MECHS.split(",");
    private String[] nonsasl_mechs = DEF_NONSASL_MECHS.split(",");


    // ~--- methods --------------------------------------------------------------

    /**
     * Describe <code>addUser</code> method here.
     *
     * @param user
     *          a <code>String</code> value
     * @param password
     *          a <code>String</code> value
     * @exception UserExistsException
     *              if an error occurs
     * @exception TigaseDBException
     *              if an error occurs
     */
    @Override
    public void addUser(BareJID user, final String password) throws UserExistsException,
            TigaseDBException {
        if (adduser_query == null) {
            return;
        }

        ResultSet rs = null;

        try {
            PreparedStatement add_user = data_repo.getPreparedStatement(user, adduser_query);

            synchronized (add_user) {
                add_user.setString(1, user.toString());
                add_user.setString(2, password);

                boolean is_result = add_user.execute();

                if (is_result) {
                    rs = add_user.getResultSet();
                }
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new UserExistsException(
                    "Error while adding user to repository, user exists?", e);
        } catch (SQLException e) {
            throw new TigaseDBException("Problem accessing repository.", e);
        } finally {
            data_repo.release(null, rs);
        }
    }

    /**
     * Describe <code>digestAuth</code> method here.
     *
     * @param user
     *          a <code>String</code> value
     * @param digest
     *          a <code>String</code> value
     * @param id
     *          a <code>String</code> value
     * @param alg
     *          a <code>String</code> value
     * @return a <code>boolean</code> value
     * @exception UserNotFoundException
     *              if an error occurs
     * @exception TigaseDBException
     *              if an error occurs
     * @exception AuthorizationException
     *              if an error occurs
     */
    @Override
    @Deprecated
    public boolean digestAuth(BareJID user, final String digest, final String id,
                              final String alg) throws UserNotFoundException, TigaseDBException,
            AuthorizationException {

        throw new AuthorizationException("Not supported.");
    }

    // ~--- get methods ----------------------------------------------------------

    /**
     * Method description
     *
     *
     * @return
     */
    @Override
    public String getResourceUri() {
        return data_repo.getResourceUri();
    }

    /**
     * <code>getUsersCount</code> method is thread safe. It uses local variable
     * for storing <code>Statement</code>.
     *
     * @return a <code>long</code> number of user accounts in database.
     */
    @Override
    public long getUsersCount() {
        return -1;
    }

    /**
     * Method description
     *
     *
     * @param domain
     *
     * @return
     */
    @Override
    public long getUsersCount(String domain) {
        return -1;
    }

    // ~--- methods --------------------------------------------------------------

    /**
     * Describe <code>initRepository</code> method here.
     *
     * @param connection_str
     *          a <code>String</code> value
     * @param params
     * @exception DBInitException
     *              if an error occurs
     */
    @Override
    public void initRepository(final String connection_str, Map<String, String> params)
            throws DBInitException {
        try {
            data_repo = RepositoryFactory.getDataRepository(null, connection_str, params);

            data_repo.initPreparedStatement(DEF_GETPASSWORD_KEY, getpassword_query);
            data_repo.initPreparedStatement(DEF_UPDATE_LAST_REQUEST_AT_KEY, updateLastRequestAt_query);


            nonsasl_mechs = getParamWithDef(params, DEF_NONSASL_MECHS_KEY, DEF_NONSASL_MECHS).split(",");
            sasl_mechs = getParamWithDef(params, DEF_SASL_MECHS_KEY, DEF_SASL_MECHS).split(",");

            if ((params != null) && (params.get("init-db") != null)) {
                initDb();
            }

        } catch (Exception e) {
            data_repo = null;

            throw new DBInitException(
                    "Problem initializing jdbc connection: " + connection_str, e);
        }
    }

    /**
     * Method description
     *
     *
     * @param user
     *
     * @throws TigaseDBException
     * @throws UserNotFoundException
     */
    @Override
    public void logout(BareJID user) throws UserNotFoundException, TigaseDBException {

        return;
    }

    /**
     * Describe <code>otherAuth</code> method here.
     *
     * @param props
     *          a <code>Map</code> value
     * @return a <code>boolean</code> value
     * @exception UserNotFoundException
     *              if an error occurs
     * @exception TigaseDBException
     *              if an error occurs
     * @exception AuthorizationException
     *              if an error occurs
     */
    @Override
    public boolean otherAuth(final Map<String, Object> props) throws UserNotFoundException,
            TigaseDBException, AuthorizationException {

        String proto = (String) props.get(PROTOCOL_KEY);

        if (proto.equals(PROTOCOL_VAL_NONSASL)) {
            String password = (String) props.get(PASSWORD_KEY);
            BareJID user_id = (BareJID) props.get(USER_ID_KEY);
            if (password != null) {
                return plainAuth(user_id, password);
            }
            String digest = (String) props.get(DIGEST_KEY);
            if (digest != null) {
                String digest_id = (String) props.get(DIGEST_ID_KEY);
                return digestAuth(user_id, digest, digest_id, "SHA");
            }
        } // end of if (proto.equals(PROTOCOL_VAL_SASL))

        throw new AuthorizationException("Protocol is not supported.");
    }

    /**
     * Describe <code>plainAuth</code> method here.
     *
     * @param user
     *          a <code>String</code> value
     * @param password
     *          a <code>String</code> value
     * @return a <code>boolean</code> value
     *
     * @throws AuthorizationException
     * @exception UserNotFoundException
     *              if an error occurs
     * @exception TigaseDBException
     *              if an error occurs
     */
    @Override
    @Deprecated
    public boolean plainAuth(BareJID user, final String password)
            throws UserNotFoundException, TigaseDBException, AuthorizationException {

        // get AppID and UserID
        //
        String JIDLocalPart = user.getLocalpart();
        Integer []appIDAndUserID = QBChatUtils.getApplicationIDAndUserIDFromUserJIDLocalPart(JIDLocalPart);
        Integer appID = appIDAndUserID[0];
        Integer userID = appIDAndUserID[1];

        // Get encrypted password and salt from QuickBlox Application DB
        //
        String[] passwordData = getPassword(user, appID, userID);


        // Crypt origin user's password using salt from DB
        //
        String calculated = calculatePassword(passwordData);


        // Try to login with plain password or encrypted
        //
        boolean success = passwordData[0].equals(calculated);


        if (log.isLoggable(Level.FINE)) {
            if(success){
                log.log(Level.FINE, "Login SUCCESS for user {0}", new Object[]{user});
            }  else{
                log.log(Level.FINE, "Login FAILED for user {0}", new Object[]{user});
            }
        }


        return (password != null) && (passwordData != null) && success;
    }

    // Implementation of tigase.db.AuthRepository

    /**
     * Describe <code>queryAuth</code> method here.
     *
     * @param authProps
     *          a <code>Map</code> value
     */
    @Override
    public void queryAuth(final Map<String, Object> authProps) {
        String protocol = (String) authProps.get(PROTOCOL_KEY);

        if (protocol.equals(PROTOCOL_VAL_NONSASL)) {
            authProps.put(RESULT_KEY, nonsasl_mechs);
        } // end of if (protocol.equals(PROTOCOL_VAL_NONSASL))

        if (protocol.equals(PROTOCOL_VAL_SASL)) {
            authProps.put(RESULT_KEY, sasl_mechs);
        } // end of if (protocol.equals(PROTOCOL_VAL_NONSASL))
    }

    /**
     * Describe <code>removeUser</code> method here.
     *
     * @param user
     *          a <code>String</code> value
     * @exception UserNotFoundException
     *              if an error occurs
     * @exception TigaseDBException
     *              if an error occurs
     */
    @Override
    public void removeUser(BareJID user) throws UserNotFoundException, TigaseDBException {
        throw new TigaseDBException("Removing user is not supported.");
    }

    /**
     * Describe <code>updatePassword</code> method here.
     *
     * @param user
     *          a <code>String</code> value
     * @param password
     *          a <code>String</code> value
     * @exception TigaseDBException
     *              if an error occurs
     * @throws UserNotFoundException
     */
    @Override
    public void updatePassword(BareJID user, final String password)
            throws UserNotFoundException, TigaseDBException {

        throw new TigaseDBException("Updating user password is not supported.");
    }

    @Override
    public String getPassword(BareJID user) throws UserNotFoundException, TigaseDBException {
        return null;
    }


    //////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////


    private String[] getPassword(BareJID user, Integer appID, Integer userID) throws TigaseDBException,
            UserNotFoundException {
        ResultSet rs = null;

        try {
            PreparedStatement preparedStatement = data_repo.getPreparedStatement(user, DEF_GETPASSWORD_KEY);

            synchronized (preparedStatement) {

                preparedStatement.setString(1, "" + userID);
                preparedStatement.setString(2, "" + appID);

                rs = preparedStatement.executeQuery();

                if (rs.next()) {
                    String[] resultArray = {rs.getString(1), rs.getString(2)};
                    return resultArray;
                } else {
                    throw new UserNotFoundException("User does not exist: " + user);
                }    // end of if (isnext) else
            }
        } catch (SQLException e) {
            throw new TigaseDBException("Problem with retrieving user password.", e);
        } finally {
            data_repo.release(null, rs);
        }
    }

    protected String getParamWithDef(Map<String, String> params, String key, String def) {
        if (params == null) {
            return def;
        }

        String result = params.get(key);

        if (result != null) {
            log.log(Level.CONFIG, "Custom query loaded for ''{0}'': ''{1}''", new Object[] {
                    key, result });
        } else {
            result = def;
            log.log(Level.CONFIG, "Default query loaded for ''{0}'': ''{1}''", new Object[] {
                    key, def });
        }

        if (result != null) {
            result = result.trim();

            if (result.isEmpty() || result.equals(NO_QUERY)) {
                result = null;
            }
        }

        return result;
    }

    private String calculatePassword(String[] passwordData){

        String calculatedPassword = ...;

        return calculatedPassword;
    }

    private void initDb() throws SQLException {
        if (initdb_query == null) {
            return;
        }

        PreparedStatement init_db = data_repo.getPreparedStatement(null, initdb_query);

        synchronized (init_db) {
            init_db.executeUpdate();
        }
    }

    public void updateLastRequestAt(BareJID user) throws UserNotFoundException, TigaseDBException {

        if (updateLastRequestAt_query == null) {
            return;
        }

        String JIDLocal = user.getLocalpart();
        Integer []result = QBChatUtils.getApplicationIDAndUserIDFromUserJIDLocalPart(JIDLocal);
        if(result == null){
            return;
        }

        try {
            PreparedStatement lastRequestAt = data_repo.getPreparedStatement(user, DEF_UPDATE_LAST_REQUEST_AT_KEY);
            if (lastRequestAt != null) {
                synchronized (lastRequestAt) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Calendar cal = Calendar.getInstance();
                    String newLastRequestAt = dateFormat.format(cal.getTime());
                    lastRequestAt.setString(1, newLastRequestAt);
                    Integer userID = result[1];
                    lastRequestAt.setString(2, ""+userID);

                    lastRequestAt.execute();
                }
            }
        } catch (SQLException e) {
            throw new TigaseDBException("Problem accessing repository.", e);
        }
    }
}
