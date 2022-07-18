package jp.ac.titech.itpro.sdl.androidfilesync;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Config {
    private final static String TAG = Config.class.getSimpleName();

    public int port;
    public String passwordDigest;
    public Set<String> backupPaths;

    public void Load(Context context){
        SharedPreferences pref = context.getSharedPreferences("AndroidFileSYNC", Context.MODE_PRIVATE);
        port = pref.getInt("port", 12345);
        passwordDigest = pref.getString("passwordDigest", "");
        backupPaths = pref.getStringSet("backupPaths", new HashSet<>());
        Log.i(TAG, "Config.Load");
        Log.i(TAG, "port:"+port);
        Log.i(TAG, "passwordDigest:"+passwordDigest);
        Log.i(TAG, "backupPaths:"+backupPaths);
    }

    public void Save(Context context){
        SharedPreferences pref = context.getSharedPreferences("AndroidFileSYNC", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        Log.i(TAG, "Config.Save");
        editor.putInt("port", port);
        editor.putString("passwordDigest", passwordDigest);
        editor.putStringSet("backupPaths", backupPaths);
        editor.commit();
    }
}
