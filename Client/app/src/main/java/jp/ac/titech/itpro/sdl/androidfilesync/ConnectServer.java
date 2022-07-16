package jp.ac.titech.itpro.sdl.androidfilesync;

import static android.content.Context.WIFI_SERVICE;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.StrictMode;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ConnectServer {
    private final static String TAG = ConnectServer.class.getSimpleName();
    Context context;
    String passwordDigest;
    String msgDigest;
    int port;
    InetAddress serverAddress;
    int localPort;

    public final static int ERROR_SUCCESS = 0;
    public final static int ERROR_AUTHORIZATION = -1;
    public final static int ERROR_TIMEOUT = -2;
    public final static int ERROR_IO = -3;

    public final static int MODE_DESKTOP = 0;
    public final static int MODE_BACKUP = 1;

    final static int BUFFER_SIZE = 1024*1024;
    final static int RETRY_COUNT = 2;   // 送信のリトライ回数


    public ConnectServer(Context context, String passwordDigest, int port){
        this.context = context;
        this.passwordDigest = passwordDigest;
        this.port = port;
    }

    public int connect(){
        serverAddress = null;
        sendBroadcast();
        if(receiveBroadCastResponse() != 0){
            return -1;
        }
        return 0;
    }

    public String getAddress(){
        if(serverAddress == null){
            connect();
        }
        if (serverAddress != null) {
            return serverAddress.getHostAddress() + ":" + port;
        }
        return null;
    }

    void sendBroadcast() {
        long unixTime = System.currentTimeMillis();
        String passWithTime = String.format("%d:%s", unixTime, passwordDigest);
        msgDigest = Encryption.sha256EncodeToString(passWithTime.getBytes(StandardCharsets.UTF_8));
        String messageStr =  String.format("%d:%s", unixTime, msgDigest);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try {
            DatagramSocket socket = new DatagramSocket();
            socket.setBroadcast(true);
            byte[] sendData = messageStr.getBytes(StandardCharsets.UTF_8);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, getBroadcastAddress(context), port);
            socket.send(sendPacket);
            localPort = socket.getLocalPort();
            Log.i(TAG,  "Broadcast packet sent to: " + getBroadcastAddress(context).getHostAddress());
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, "IOException: " + e.getMessage());
        }
    }

    static InetAddress getBroadcastAddress(Context context) throws IOException {
        WifiManager wifi = (WifiManager) context.getSystemService(WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }

    int receiveBroadCastResponse(){
        try {
            ServerSocket serverSocket = new ServerSocket(localPort);
            serverSocket.setSoTimeout(3000);
            Socket socket = serverSocket.accept();

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String data = reader.readLine();

            if(!data.equals(Encryption.sha256EncodeToString(msgDigest+passwordDigest))){
                Log.e(TAG, "auth error");
                msgDigest = null;
                return ERROR_AUTHORIZATION;
            }

            serverAddress = socket.getInetAddress();
        } catch (SocketTimeoutException e){
            e.printStackTrace();
            Log.e(TAG, "receiveBroadCastResponse: タイムアウト");
            return ERROR_TIMEOUT;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "receiveBroadCastResponse: IOエラー");
            return ERROR_IO;
        }
        return ERROR_SUCCESS;
    }

    public int SendFile(URL url, String path, int mode){
        byte[] fileBuffer = new byte[BUFFER_SIZE];
        int bufferSize = 0;
        FileInputStream fileSteam = null;

        try {
            File file = new File(path);
            fileSteam = new FileInputStream(file);

            int splitIndex = 0;
            for(; (bufferSize = fileSteam.read(fileBuffer)) > 0; splitIndex++) {
                SendFileData(splitIndex, url, fileBuffer,
                        bufferSize, file.length(), path,
                        file.lastModified(), mode);
            }
            SendFileData(splitIndex, url, fileBuffer,
                    0, 0, path,
                    file.lastModified(), mode);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return ERROR_IO;
        } catch (IOException e) {
            e.printStackTrace();
            return ERROR_IO;
        }
        return ERROR_SUCCESS;
    }

    int SendFileData(int splitIndex, URL url, byte[] fileBuffer,
                             int bufferSize, long fileSize, String path,
                             long lastModified, int mode) {
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
                connection.setRequestProperty("Sha-256", Encryption.sha256EncodeToString(fileBuffer, bufferSize));
                switch(mode){
                    case MODE_DESKTOP:
                        connection.setRequestProperty("Mode", "DESKTOP");
                        break;
                    case MODE_BACKUP:
                        connection.setRequestProperty("Mode", "BACKUP");
                        break;
                }
                byte[] body = Encryption.base64Encode(fileBuffer, bufferSize);
                connection.setFixedLengthStreamingMode(body.length);
                OutputStream httpStream = connection.getOutputStream();

                httpStream.write(body, 0, body.length);
                connection.connect();

                Log.i(TAG, "Split:"+splitIndex);
                Log.i(TAG, "File-Path:"+path);
                Log.i(TAG, "File-Size:"+fileSize);
                Log.i(TAG, "Last-Modified:"+lastModified);
                Log.i(TAG, "SHA256:"+Encryption.sha256EncodeToString(fileBuffer, bufferSize));

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
}
