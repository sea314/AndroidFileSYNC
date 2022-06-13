package jp.ac.titech.itpro.sdl.androidfilesync;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();
    private String url = "http://10.166.125.50:12345/file";
    String necessaryPermissions[] = {
        Manifest.permission.INTERNET,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_WIFI_STATE,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        String action = intent.getAction();

        if(CheckNecessaryPermissions() == PackageManager.PERMISSION_GRANTED){
            if (Intent.ACTION_SEND.equals(action)) {    // 共有(単一ファイル)
                handleSendFile(intent);
            } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {        // 共有(複数ファイル)
                handleSendMultipleFiles(intent);
            } else {                                     // その他
                // todo 設定画面
                ConnectServer connectServer = new ConnectServer(getApplicationContext(), "password", 12345);
                connectServer.connect();
                Log.i(TAG, "Server:"+connectServer.getAddress());

                Intent newIntent = new Intent(this, TestService.class);
                startService(newIntent);
            }
        }
        else{
            ActivityCompat.requestPermissions(this, necessaryPermissions, 1);
        }
    }


    void handleSendFile(Intent intent) {
        Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if(uri == null){
            Log.e(TAG, "uriが開けません");
            return;
        }

        String path = UriPath.getPathFromUri(this, uri);
        if (path == null) {
            Log.e(TAG, "uriをファイル名に変換できません:\""+uri+"\"");
            return;
        }

        Intent newIntent = new Intent(this, ActionSendService.class);
        newIntent.putStringArrayListExtra(ActionSendService.EXTRA_ARG_PATHS, new ArrayList<>(Arrays.asList(path)));
        newIntent.putExtra(ActionSendService.EXTRA_ARG_URL, url);
        newIntent.putExtra(ActionSendService.EXTRA_ARG_MODE, "DESKTOP");
        startService(newIntent);
    }

    void handleSendMultipleFiles(Intent intent) {
        ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if(uris == null){
            Log.e(TAG, "uriが開けません");
            return;
        }

        ArrayList<String> paths = new ArrayList<>();
        for(Uri uri : uris){
            String path = UriPath.getPathFromUri(this, uri);
            if(path == null){
                Log.e(TAG, "uriをファイル名に変換できません:\""+uri+"\"");
                continue;
            }
            paths.add(path);
        }

        Intent newIntent = new Intent(this, ActionSendService.class);
        newIntent.putStringArrayListExtra(ActionSendService.EXTRA_ARG_PATHS, paths);
        newIntent.putExtra(ActionSendService.EXTRA_ARG_URL, url);
        newIntent.putExtra(ActionSendService.EXTRA_ARG_MODE, "DESKTOP");
        startService(newIntent);
    }


    int CheckNecessaryPermissions(){
        int result[] = new int[necessaryPermissions.length];

        for(int i=0; i<necessaryPermissions.length; i++){
            result[i] = checkSelfPermission(necessaryPermissions[i]);
            Log.i(TAG, necessaryPermissions[i]+":"+checkSelfPermission(necessaryPermissions[i]));
        }

        return Arrays.stream(result).allMatch(a -> a==PackageManager.PERMISSION_GRANTED) ? PackageManager.PERMISSION_GRANTED : PackageManager.PERMISSION_DENIED;
    }
}