package jp.ac.titech.itpro.sdl.androidfilesync;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        String action = intent.getAction();

        if (Intent.ACTION_SEND.equals(action)) {    // 共有(単一ファイル)
            handleSendFile(intent);
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {        // 共有(複数ファイル)
            handleSendMultipleFiles(intent);
        } else{                                     // その他
            // 設定画面を開く

        }
    }

    void handleSendFile(Intent intent) {
        Uri uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (uri != null) {
            String path = UriToPath.getPathFromUri(this, uri);
            Intent newintent = new Intent(this, ActionSendService.class);
            newintent.putStringArrayListExtra(ActionSendService.EXTRA_ARG_PATHS, new ArrayList<>(Arrays.asList(path)));
            startService(newintent);
        }
        else{
            Log.d(TAG, "uri is not open");
        }
    }

    void handleSendMultipleFiles(Intent intent) {
        ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (uris != null) {
            ArrayList<String> paths = new ArrayList<>();
            for(Uri uri : uris){
                paths.add( UriToPath.getPathFromUri(this, uri));
            }
            Intent newintent = new Intent(this, ActionSendService.class);
            newintent.putStringArrayListExtra(ActionSendService.EXTRA_ARG_PATHS, paths);
            startService(newintent);
        }else{
            Log.d(TAG, "uri is not open");
        }
    }
}