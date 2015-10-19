package main.java.com.quickblox.chat.customobjectsplugin;

import main.java.com.quickblox.chat.customobjectsplugin.model.Message;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by QuickBlox team on 1/1/15.
 */
public class CustomObjectPostSender implements Runnable {

    public static final String POST = "POST";
    public static final String ENDPOINT_URL = "%s/data/%s.json";
    public static final String QB_TOKEN = "QB-Token";

    private static final Logger log = Logger.getLogger(CustomObjectPostSender.class.getName());

    private String domainUrl;
    private String token;
    private String className;
    private Message message;

    public CustomObjectPostSender(String domainUrl, String token, String className, Message message) {
        this.domainUrl = domainUrl;
        this.token = token;
        this.className = className;
        this.message = message;
    }

    @Override
    public void run() {
        try {
            String url = String.format(ENDPOINT_URL, domainUrl, className);
            URL obj = new URL(url);

            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod(POST);
            con.setRequestProperty(QB_TOKEN, token);
            con.setDoOutput(true);

            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            String data = message.toString();

            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, "url: " + url + ", data: " + data);
            }

            wr.write(data.getBytes("UTF-8"));
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();
            String responseMessage = con.getResponseMessage();
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, "Response Code: " + responseCode + ", responseMessage: " + responseMessage);
            }
            con.disconnect();
        } catch (MalformedURLException e) {
            log.log(Level.SEVERE, e.toString());
        } catch (IOException e) {
            log.log(Level.SEVERE, e.toString());
        }
    }

}
