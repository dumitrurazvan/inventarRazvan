package ro.cnpr.inventar.prefs;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefsManager {

    private static final String PREF_NAME = "inventar_prefs";
    private static final String KEY_SERVER_IP = "server_ip";
    private static final String KEY_SERVER_PORT = "server_port";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static void setServerIp(Context context, String ip) {
        getPrefs(context).edit().putString(KEY_SERVER_IP, ip).apply();
    }

    public static String getServerIp(Context context) {
        return getPrefs(context).getString(KEY_SERVER_IP, "");
    }

    public static void setServerPort(Context context, String port) {
        getPrefs(context).edit().putString(KEY_SERVER_PORT, port).apply();
    }

    public static String getServerPort(Context context) {
        return getPrefs(context).getString(KEY_SERVER_PORT, "");
    }
}
