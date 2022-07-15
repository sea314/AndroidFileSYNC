package jp.ac.titech.itpro.sdl.androidfilesync;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class Config {
    private final static String TAG = Config.class.getSimpleName();

    public int port;
    public String passwordDigest;

    public void Load(Context context){
        SharedPreferences pref = context.getSharedPreferences("AndroidFileSYNC", Context.MODE_PRIVATE);
        port = pref.getInt("port", 12345);
        passwordDigest = pref.getString("passwordDigest", "");
        Log.i(TAG, "Config.Load");
        Log.i(TAG, "port:"+port);
        Log.i(TAG, "passwordDigest:"+passwordDigest);
    }

    public void Save(Context context){
        SharedPreferences pref = context.getSharedPreferences("AndroidFileSYNC", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt("port", port);
        editor.putString("passwordDigest", passwordDigest);
        editor.commit();

        Log.i(TAG, "Config.Save");
    }
}
