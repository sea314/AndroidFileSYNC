package jp.ac.titech.itpro.sdl.androidfilesync;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class SendFileService extends IntentService {
    private final static String TAG = SendFileService.class.getSimpleName();
    public static final String ACTION_SEND_FILE = "jp.ac.titech.itpro.sdl.androidfilesync.action.SEND_FILE";
    private static final String ACTION_BACKUP = "jp.ac.titech.itpro.sdl.androidfilesync.action.BACKUP";

    public static final String EXTRA_PATHS = "jp.ac.titech.itpro.sdl.androidfilesync.extra.PATHS";
    public static final String EXTRA_PASSWORD_DIGEST = "jp.ac.titech.itpro.sdl.androidfilesync.extra.PASSWORD_DIGEST";
    public static final String EXTRA_PORT = "jp.ac.titech.itpro.sdl.androidfilesync.extra.PORT";

    private static boolean isRunning = false;

    ConnectServer connection;

    public SendFileService() {
        super(TAG);
    }

    public static void startActionSendFile(Context context, ArrayList<String> paths, int port, String passwordDigest) {
        if(isRunning){
            return;
        }
        isRunning = true;
        Intent intent = new Intent(context, SendFileService.class);
        intent.setAction(ACTION_SEND_FILE);
        intent.putStringArrayListExtra(EXTRA_PATHS, paths);
        intent.putExtra(EXTRA_PASSWORD_DIGEST, passwordDigest);
        intent.putExtra(EXTRA_PORT, port);
        context.startService(intent);
    }

    public static void startActionBackup(Context context, ArrayList<String> paths, int port, String passwordDigest) {
        if(isRunning){
            return;
        }
        isRunning = true;
        Intent intent = new Intent(context, SendFileService.class);
        intent.setAction(ACTION_BACKUP);
        intent.putStringArrayListExtra(EXTRA_PATHS, paths);
        intent.putExtra(EXTRA_PASSWORD_DIGEST, passwordDigest);
        intent.putExtra(EXTRA_PORT, port);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent in " + Thread.currentThread());
        Context context = getApplicationContext();

        if(intent == null){
            return;
        }

        String action = intent.getAction();
        ArrayList<String> paths = intent.getStringArrayListExtra(EXTRA_PATHS);
        String password_digest = intent.getStringExtra(EXTRA_PASSWORD_DIGEST);
        int port = intent.getIntExtra(EXTRA_PORT, 0);

        connection = new ConnectServer(context, password_digest, port);
        int connErr = connection.connect();

        if(connErr != ConnectServer.ERROR_SUCCESS){
            Log.e(TAG, "サーバー接続失敗");
            switch (connErr){
                case ConnectServer.ERROR_AUTHORIZATION:
                    // todo:broadcastで通知
                    break;
                case ConnectServer.ERROR_TIMEOUT:
                    // todo:broadcastで通知
                    break;
                case ConnectServer.ERROR_IO:
                    // todo:broadcastで通知
                    break;
            }
            return;
        }
        Log.i(TAG, connection.getAddress());

        if (ACTION_BACKUP.equals(action)) {
            if (paths != null) {
                Log.d(TAG, "paths:"+paths);
                connection.backup(paths);
            }
            else{
                Log.d(TAG, "uri is not open");
            }
        }
        else if(ACTION_SEND_FILE.equals(action)){
            if (paths != null) {
                Log.d(TAG, "paths:"+paths);

                for(String path : paths) {
                    connection.sendFile(path, ConnectServer.MODE_DESKTOP);
                }
            }
            else{
                Log.d(TAG, "uri is not open");
            }

        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate in " + Thread.currentThread());
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy in " + Thread.currentThread());
        isRunning = false;
        super.onDestroy();
    }
}
