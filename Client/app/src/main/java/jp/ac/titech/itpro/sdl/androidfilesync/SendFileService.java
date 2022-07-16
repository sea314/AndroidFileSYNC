package jp.ac.titech.itpro.sdl.androidfilesync;

import android.app.IntentService;
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
    public final static String EXTRA_ARG_PATHS = "ARG_PATHS";
    public final static String EXTRA_ARG_MODE = "ARG_MODE";
    public final static String EXTRA_ARG_PASSWORD_DIGEST = "ARG_PASSWORD_DIGEST";
    public final static String EXTRA_ARG_PORT = "ARG_PORT";

    ConnectServer connection;

    public SendFileService() {
        super(TAG);
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent in " + Thread.currentThread());
        ArrayList<String> paths = intent.getStringArrayListExtra(EXTRA_ARG_PATHS);
        int mode = intent.getIntExtra(EXTRA_ARG_MODE, ConnectServer.MODE_DESKTOP);
        String password_digest = intent.getStringExtra(EXTRA_ARG_PASSWORD_DIGEST);
        int port = intent.getIntExtra(EXTRA_ARG_PORT, 0);

        connection = new ConnectServer(getApplicationContext(), password_digest, port);
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

        URL url;
        try {
            url = new URL("http://"+connection.getAddress()+"/file");
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return;
        }

        if (paths != null) {
            Log.d(TAG, "paths:"+paths);

            for(String path : paths) {
                connection.SendFile(url, path, mode);
            }
        }
        else{
            Log.d(TAG, "uri is not open");
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
        super.onDestroy();
    }
}
