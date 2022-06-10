package jp.ac.titech.itpro.sdl.androidfilesync;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();
    private String url = "http://10.166.125.50:12345/file";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        String action = intent.getAction();

        CheckPermissions();

        if (Intent.ACTION_SEND.equals(action)) {    // 共有(単一ファイル)
            handleSendFile(intent);
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {        // 共有(複数ファイル)
            handleSendMultipleFiles(intent);
        } else {                                     // その他
            // 設定画面を開く
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            WifiInfo w_info = wifiManager.getConnectionInfo();
            Log.i(TAG, "SSID:" + ((WifiInfo) w_info).getSSID());
            Log.i(TAG, "BSSID:" + w_info.getBSSID());
            Log.i(TAG, "IP Address:" + w_info.getIpAddress());
            Log.i(TAG, "Network ID:" + w_info.getNetworkId());
            Log.i(TAG, "Link Speed:" + w_info.getLinkSpeed());

            if (w_info.getNetworkId() == -1) {
                Log.d(TAG, "network is not available");
            }
        }
    }

    void CheckPermissions(){
        Log.i(TAG, "Permission.INTERNET:" + checkSelfPermission(Manifest.permission.INTERNET));
        Log.i(TAG, "Permission.READ_EXTERNAL_STORAGE:" + checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE));
        Log.i(TAG, "Permission.ACCESS_WIFI_STATE:" + checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE));
        Log.i(TAG, "Permission.ACCESS_COARSE_LOCATION:" + checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION));
        Log.i(TAG, "Permission.ACCESS_FINE_LOCATION:" + checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION));

    }

    void handleSendFile(Intent intent) {
        // todo 権限取得：storage, internet

        Uri uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if(uri == null){
            Log.e(TAG, "uriが開けません");
            return;
        }

        String path = UriToPath.getPathFromUri(this, uri);
        if (path == null) {
            Log.e(TAG, "uriをファイル名に変換できません:\""+uri+"\"");
            return;
        }

        Intent newintent = new Intent(this, ActionSendService.class);
        newintent.putStringArrayListExtra(ActionSendService.EXTRA_ARG_PATHS, new ArrayList<>(Arrays.asList(path)));
        newintent.putExtra(ActionSendService.EXTRA_ARG_URL, url);
        newintent.putExtra(ActionSendService.EXTRA_ARG_MODE, "DESKTOP");
        startService(newintent);
    }

    void handleSendMultipleFiles(Intent intent) {
        // todo 権限取得：storage, internet
        ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if(uris == null){
            Log.e(TAG, "uriが開けません");
        }

        ArrayList<String> paths = new ArrayList<>();
        for(Uri uri : uris){
            String path = UriToPath.getPathFromUri(this, uri);
            if(path == null){
                Log.e(TAG, "uriをファイル名に変換できません:\""+uri+"\"");
                continue;
            }
            paths.add(path);
        }

        Intent newintent = new Intent(this, ActionSendService.class);
        newintent.putStringArrayListExtra(ActionSendService.EXTRA_ARG_PATHS, paths);
        newintent.putExtra(ActionSendService.EXTRA_ARG_URL, url);
        newintent.putExtra(ActionSendService.EXTRA_ARG_MODE, "DESKTOP");
        startService(newintent);
    }


}