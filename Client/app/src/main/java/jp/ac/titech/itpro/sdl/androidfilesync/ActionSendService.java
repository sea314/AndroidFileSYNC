package jp.ac.titech.itpro.sdl.androidfilesync;

import android.app.IntentService;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.util.Base64;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

public class ActionSendService extends IntentService {
    private final static String TAG = ActionSendService.class.getSimpleName();
    public final static String EXTRA_ARG_PATHS = "ARG_PATHS";
    public final static String EXTRA_ARG_URL = "ARG_URL";
    public final static String EXTRA_ARG_MODE = "ARG_MODE";
    final static int BUFFER_SIZE = 1024*10;
    final static int RETRY_COUNT = 2;   // 送信のリトライ回数


    public ActionSendService() {
        super(TAG);
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent in " + Thread.currentThread());
        ArrayList<String> paths = intent.getStringArrayListExtra(EXTRA_ARG_PATHS);
        String urlString = intent.getStringExtra(EXTRA_ARG_URL);
        String mode = intent.getStringExtra(EXTRA_ARG_MODE);

        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo w_info = wifiManager.getConnectionInfo();
        Log.i(TAG, "SSID:"+ ((WifiInfo) w_info).getSSID());
        Log.i(TAG, "BSSID:"+w_info.getBSSID());
        Log.i(TAG, "IP Address:"+w_info.getIpAddress());
        Log.i(TAG, "Network ID:"+w_info.getNetworkId());
        Log.i(TAG, "Link Speed:"+w_info.getLinkSpeed());



        if (paths != null) {
            Log.d(TAG, "paths:"+paths);

            for(String path : paths) {
                SendFile(url, path, mode);
            }
        }
        else{
            Log.d(TAG, "uri is not open");
        }
    }



    private int SendFile(URL url, String path, String mode){
        byte[] fileBuffer = new byte[BUFFER_SIZE];
        int bufferSize = 0;
        FileInputStream fileSteam = null;

        try {
            File file = new File(path);
            fileSteam = new FileInputStream(file);

            for(int splitIndex = 0; (bufferSize = fileSteam.read(fileBuffer)) > 0; splitIndex++) {
                SendFileData(splitIndex, url, fileBuffer,
                        bufferSize, file.length(), path,
                        file.lastModified(), mode);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return -1;
        } catch (IOException e) {
            e.printStackTrace();
            return -2;
        }
        return 0;
    }

    private int SendFileData(int splitIndex, URL url, byte[] fileBuffer,
                             int bufferSize, long fileSize, String path,
                             long lastModified, String mode) {
        HttpURLConnection connection = null;

        try{
            for(int retryCount =0; retryCount < RETRY_COUNT; retryCount++){
                connection = (HttpURLConnection)url.openConnection();
                connection.setConnectTimeout(3000); // タイムアウト 3 秒
                connection.setReadTimeout(3000);
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);       // body有効化
                connection.setInstanceFollowRedirects(false);   // リダイレクト無効化
                connection.setRequestProperty("Content-Type", "application/octet-stream");  // バイナリ全般
                connection.setRequestProperty("Split", String.valueOf(splitIndex));
                connection.setRequestProperty("File-Path", path);
                connection.setRequestProperty("File-Size", String.valueOf(fileSize));
                connection.setRequestProperty("Last-Modified", String.valueOf(lastModified));
                connection.setRequestProperty("Sha-256", calculateSha256(fileBuffer, bufferSize));
                connection.setRequestProperty("Mode", mode);
                byte[] body = base64encode(fileBuffer, bufferSize);
                connection.setFixedLengthStreamingMode(body.length);
                OutputStream httpStream = connection.getOutputStream();

                httpStream.write(body, 0, body.length);
                connection.connect();

                Log.i(TAG, "Split:"+splitIndex);
                Log.i(TAG, "File-Path:"+path);
                Log.i(TAG, "File-Size:"+fileSize);
                Log.i(TAG, "Last-Modified:"+lastModified);
                Log.i(TAG, "SHA256:"+calculateSha256(fileBuffer, bufferSize));

                // レスポンスコードの確認します。
                int responseCode = connection.getResponseCode();
                String response = connection.getResponseMessage();
                connection.disconnect();

                switch(responseCode){
                    case HttpURLConnection.HTTP_OK:
                        Log.i(TAG, "HTTP_OK:"+response);
                        return 0;

                    case HttpURLConnection.HTTP_BAD_REQUEST:
                        Log.e(TAG, "HTTP_BAD_REQUEST:"+response);
                        continue;

                    case HttpURLConnection.HTTP_SERVER_ERROR:
                        Log.e(TAG, "HTTP_SERVER_ERROR:"+response);
                        continue;

                    default:
                        Log.e(TAG, responseCode+":"+response);
                        return -4;
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return -1;
        } catch (IOException e) {
            e.printStackTrace();
            return -2;
        }
        return -3;
    }

    // sha256ハッシュ値を計算
    private String calculateSha256(byte[] fileBuffer, int bufferSize){
        try {
            byte[] cipher_byte;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(fileBuffer, 0, bufferSize);
            cipher_byte = md.digest();
            return Base64.encodeToString(cipher_byte, Base64.DEFAULT);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    private byte[] base64encode(byte[] bytes, int size){
        return Base64.encode(Arrays.copyOf(bytes, size), Base64.DEFAULT);
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
