package jp.ac.titech.itpro.sdl.androidfilesync;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class ActionSendService extends IntentService {
    private final static String TAG = ActionSendService.class.getSimpleName();
    public final static String EXTRA_ARG_PATHS = "ARG_PATHS";
    public final static String EXTRA_ARG_URL = "ARG_URL";

    public ActionSendService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent in " + Thread.currentThread());
        ArrayList<String> paths = intent.getStringArrayListExtra(EXTRA_ARG_PATHS);
        byte[] filebuffer = new byte[1024*1024];
        int buffersize = 0;

        String url = intent.getStringExtra(EXTRA_ARG_URL);

        if (paths != null) {
            Log.d(TAG, "paths:"+paths);

            for(String path : paths){
                File file = new File(path);
                Log.d(TAG, "更新日時："+file.lastModified());
                try {
                    FileInputStream steam = new FileInputStream(file);

                    while((buffersize = steam.read(filebuffer)) > 0){
                        // todo ここにファイル転送処理

                    }
                    steam.close();

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
