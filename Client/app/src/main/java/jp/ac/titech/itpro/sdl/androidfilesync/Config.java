package jp.ac.titech.itpro.sdl.androidfilesync;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

public class Config {
    private final static String TAG = Config.class.getSimpleName();
    private final static String KEY_APPNAME = "AndroidFileSYNC";
    private final static String KEY_PORT = "port";
    private final static String KEY_PASSWORD_DIGEST = "passwordDigest";
    private final static String KEY_BACKUP_PATHS = "backupPaths";
    private final static String KEY_AUTO_BACKUP = "autoBackup";


    private int port;
    private String passwordDigest;
    private ArrayList<String> backupPaths = new ArrayList<>();
    private boolean autoBackup;

    public void Load(Context context){
        SharedPreferences pref = context.getSharedPreferences(KEY_APPNAME, Context.MODE_PRIVATE);
        port = pref.getInt(KEY_PORT, 12345);
        passwordDigest = pref.getString(KEY_PASSWORD_DIGEST, "");
        backupPaths.clear();
        backupPaths.addAll(pref.getStringSet(KEY_BACKUP_PATHS, new HashSet<>()));
        autoBackup = pref.getBoolean(KEY_AUTO_BACKUP, false);

        Log.i(TAG, "Config.Load");
        Log.i(TAG, "port:"+port);
        Log.i(TAG, "passwordDigest:"+passwordDigest);
        Log.i(TAG, "backupPaths:"+backupPaths);
        Log.i(TAG, "autoBackup:"+autoBackup);
    }

    public void Save(Context context){
        SharedPreferences pref = context.getSharedPreferences(KEY_APPNAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        Log.i(TAG, "Config.Save");
        editor.putInt(KEY_PORT, port);
        editor.putString(KEY_PASSWORD_DIGEST, passwordDigest);
        editor.putStringSet(KEY_BACKUP_PATHS, new HashSet<>(backupPaths));
        editor.putBoolean(KEY_AUTO_BACKUP, autoBackup);
        editor.commit();
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPasswordDigest() {
        return passwordDigest;
    }

    public void setPasswordDigest(String passwordDigest) {
        this.passwordDigest = passwordDigest;
    }

    public ArrayList<String> getBackupPaths() {
        return new ArrayList<>(backupPaths);
    }

    public void setBackupPaths(ArrayList<String> backupPaths) {
        this.backupPaths = new ArrayList<>(backupPaths);
        Collections.sort(this.backupPaths);
        // 包含関係にあるpathは統合する
        for(int i=0;i+1<this.backupPaths.size();){
            String s = this.backupPaths.get(i);
            String l = this.backupPaths.get(i+1);

            if(l.equals(s) || l.startsWith(s+"/")){
                this.backupPaths.remove(i+1);
            }
            else{
                i++;
            }
        }
    }

    public boolean isAutoBackup() {
        return autoBackup;
    }

    public void setAutoBackup(boolean autoBackup) {
        this.autoBackup = autoBackup;
    }
}
