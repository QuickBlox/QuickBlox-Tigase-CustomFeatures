package main.java.com.quickblox.chat.customobjectsplugin.util;

/**
 * Created by QuickBlox team on 1/1/15.
 */
public class TextUtil {

    public static final String UTF8 = "UTF-8";

    private TextUtil() {
    }

    public static boolean isEmpty(CharSequence str) {
        if (str == null || str.length() == 0) {
            return true;
        } else {
            return false;
        }
    }
}
