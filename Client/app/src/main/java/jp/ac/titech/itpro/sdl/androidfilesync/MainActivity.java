package jp.ac.titech.itpro.sdl.androidfilesync;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();
    private String url = "http://10.166.125.50:12345/file";
    String permissions[] = {
            Manifest.permission.INTERNET,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_MULTICAST_STATE,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        String action = intent.getAction();

        if(CheckPermissions() == PackageManager.PERMISSION_GRANTED){
            if (Intent.ACTION_SEND.equals(action)) {    // 共有(単一ファイル)
                handleSendFile(intent);
            } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {        // 共有(複数ファイル)
                handleSendMultipleFiles(intent);
            } else {                                     // その他
                // 設定画面を開く
                WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                WifiInfo w_info = wifiManager.getConnectionInfo();
                Log.i(TAG, "SSID:" + w_info.getSSID());
                Log.i(TAG, "BSSID:" + w_info.getBSSID());
                Log.i(TAG, "IP Address:" + w_info.getIpAddress());
                Log.i(TAG, "Network ID:" + w_info.getNetworkId());
                Log.i(TAG, "Link Speed:" + w_info.getLinkSpeed());

                if (w_info.getNetworkId() == -1) {
                    Log.d(TAG, "network is not available");
                }

                Intent newIntent = new Intent(this, TestService.class);
                startService(newIntent);
            }
        }
        else{
            ConnectServer connectServer = new ConnectServer(getApplicationContext(), "password", 12345);
            connectServer.connect();

            requestPermissions();
        }
    }


    void handleSendFile(Intent intent) {
        // todo 権限取得：storage, internet

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
        // todo 権限取得：storage, internet
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


    int CheckPermissions(){
        int result[] = new int[permissions.length];

        for(int i=0; i<permissions.length; i++){
            result[i] = checkSelfPermission(permissions[i]);
            Log.i(TAG, permissions[i]+":"+checkSelfPermission(permissions[i]));
        }

        return Arrays.stream(result).allMatch(a -> a==PackageManager.PERMISSION_GRANTED) ? PackageManager.PERMISSION_GRANTED : PackageManager.PERMISSION_DENIED;
    }

    void requestPermissions(){
        if (CheckPermissions() != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "持っていない権限あり");
            // ユーザーはパーミッションを許可していない
            ActivityCompat.requestPermissions(this, permissions, 1);
        }
    }
}