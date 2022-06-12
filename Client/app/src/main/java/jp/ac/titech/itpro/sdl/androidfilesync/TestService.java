package jp.ac.titech.itpro.sdl.androidfilesync;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class TestService extends IntentService {
    private final static String TAG = TestService.class.getSimpleName();

    public TestService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
//        ConnectServer.sendBroadcast(getApplicationContext(), "test");
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
