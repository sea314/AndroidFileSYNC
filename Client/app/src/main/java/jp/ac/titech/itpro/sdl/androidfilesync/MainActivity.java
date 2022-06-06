package jp.ac.titech.itpro.sdl.androidfilesync;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        String action = intent.getAction();

        if (Intent.ACTION_SEND.equals(action)) {
            handleSendFile(intent); // Handle single image being sent
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            handleSendMultipleFiles(intent); // Handle multiple images being sent
        } else{

        }


    }

    void handleSendFile(Intent intent) {
        Uri uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (uri != null) {
            Log.d(TAG, "handleSendFile:"+uri);
            // todo ファイル転送
        }
    }

    void handleSendMultipleFiles(Intent intent) {
        ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (uris != null) {
            Log.d(TAG, "handleSendMultipleFiles:"+uris);
            // todo ファイル転送
        }
    }
}