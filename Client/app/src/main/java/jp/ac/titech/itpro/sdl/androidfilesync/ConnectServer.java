package jp.ac.titech.itpro.sdl.androidfilesync;

import static android.content.Context.WIFI_SERVICE;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.StrictMode;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ConnectServer {
    private final static String TAG = ConnectServer.class.getSimpleName();
    static final int randomBytesSize = 8;
    Context context;
    String passwordDigest;
    String msgDigest;
    int port;
    InetAddress serverAddress;
    int localPort;
    RSACipher rsaCipher;
    AESCipher aesCipher;


    public final static int ERROR_SUCCESS = 0;
    public final static int ERROR_AUTHORIZATION = -1;
    public final static int ERROR_TIMEOUT = -2;
    public final static int ERROR_IO = -3;
    public final static int ERROR_FILE_OPEN = -4;

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
        rsaCipher = new RSACipher();
        rsaCipher.initialize();
        sendBroadcast();
        return receiveBroadCastResponse();
    }

    public String getServerAddress(){
        if(serverAddress == null){
            connect();
        }
        if (serverAddress != null) {
            return serverAddress.getHostAddress() + ":" + port;
        }
        return null;
    }

    static String getWifiIPAddress(Context context) {
        WifiManager manager = (WifiManager)context.getSystemService(WIFI_SERVICE);
        WifiInfo info = manager.getConnectionInfo();
        int ipAddr = info.getIpAddress();
        return String.format("%d.%d.%d.%d",
                (ipAddr>>0)&0xff, (ipAddr>>8)&0xff, (ipAddr>>16)&0xff, (ipAddr>>24)&0xff);
    }

    void sendBroadcast() {
        SecureRandom random = new SecureRandom();
        byte[] randomBytes = new byte[randomBytesSize];
        byte[] publicKeyBytes = rsaCipher.getPublicKeyBytes();
        String idAddr = getWifiIPAddress(context);

        // ハッシュ値計算　(乱数)(パスワード)(公開鍵)(IPアドレス)のハッシュ
        random.nextBytes(randomBytes);
        ByteBuffer buffer = ByteBuffer.allocate(randomBytes.length+passwordDigest.length()+publicKeyBytes.length+idAddr.length());
        buffer.put(randomBytes);
        buffer.put(passwordDigest.getBytes(StandardCharsets.UTF_8));
        buffer.put(publicKeyBytes);
        buffer.put(idAddr.getBytes(StandardCharsets.UTF_8));
        msgDigest = Hash.sha256EncodeToString(buffer.array());
        String randomBytesBase64 = Base64.encodeToString(randomBytes, Base64.NO_WRAP);
        String publicKeyBase64 = Base64.encodeToString(publicKeyBytes, Base64.NO_WRAP);

        // メッセージ本体　(アプリ名),(バージョン),(base64乱数),(base64公開鍵),(base64メッセージハッシュ)
        String messageStr =  String.format("FileSYNC,0.1,%s,%s,%s", randomBytesBase64, publicKeyBase64, msgDigest);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try {
            DatagramSocket socket = new DatagramSocket();
            socket.setBroadcast(true);
            byte[] sendData = messageStr.getBytes(StandardCharsets.UTF_8);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, getBroadcastAddress(context), port);
            socket.send(sendPacket);
            localPort = socket.getLocalPort();
            socket.close();
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

            if(!data.equals(Hash.sha256EncodeToString(msgDigest+passwordDigest))){
                Log.e(TAG, "auth error");
                msgDigest = null;
                return ERROR_AUTHORIZATION;
            }

            serverAddress = socket.getInetAddress();
            serverSocket.close();
        } catch (SocketTimeoutException e){
            e.printStackTrace();
            Log.e(TAG, "receiveBroadCastResponse: タイムアウト");
            return ERROR_TIMEOUT;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "receiveBroadCastResponse: IOエラー");
            return ERROR_IO;
        }
        Log.i(TAG, "server address:"+getServerAddress());
        return ERROR_SUCCESS;
    }

    public int sendFile(String localPath, String serverPath, int mode){
        byte[] fileBuffer = new byte[BUFFER_SIZE];
        int bufferSize = 0;
        FileInputStream fileSteam = null;
        try {
            URL url = new URL("http://"+getServerAddress()+"/file");
            File file = new File(localPath);
            fileSteam = new FileInputStream(file);

            int splitIndex = 0;
            for(; (bufferSize = fileSteam.read(fileBuffer)) > 0; splitIndex++) {
                sendFileData(splitIndex, url, fileBuffer,
                        bufferSize, file.length(), serverPath,
                        file.lastModified(), mode);
            }
            sendFileData(splitIndex, url, fileBuffer,
                    0, 0, serverPath,
                    file.lastModified(), mode);
            fileSteam.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return ERROR_FILE_OPEN;
        } catch (IOException e) {
            e.printStackTrace();
            return ERROR_IO;
        }
        return ERROR_SUCCESS;
    }

    private int sendFileData(int splitIndex, URL url, byte[] fileBuffer,
                             int bufferSize, long fileSize, String serverPath,
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
                connection.setRequestProperty("File-Path", serverPath);
                connection.setRequestProperty("File-Size", String.valueOf(fileSize));
                connection.setRequestProperty("Last-Modified", String.valueOf(lastModified));
                connection.setRequestProperty("Sha-256", Hash.sha256EncodeToString(fileBuffer, bufferSize));
                switch(mode){
                    case MODE_DESKTOP:
                        connection.setRequestProperty("Mode", "DESKTOP");
                        break;
                    case MODE_BACKUP:
                        connection.setRequestProperty("Mode", "BACKUP");
                        break;
                }
                byte[] body = Hash.base64Encode(fileBuffer, bufferSize);
                connection.setFixedLengthStreamingMode(body.length);
                OutputStream httpStream = connection.getOutputStream();

                httpStream.write(body, 0, body.length);
                connection.connect();

                // レスポンスコードの確認します。
                int responseCode = connection.getResponseCode();
                String response = connection.getResponseMessage();
                connection.disconnect();

                switch(responseCode){
                    case HttpURLConnection.HTTP_OK:
                        return ERROR_SUCCESS;

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
            return ERROR_FILE_OPEN;
        } catch (IOException e) {
            e.printStackTrace();
            return ERROR_IO;
        }
        return ERROR_TIMEOUT;
    }

    public ArrayList<ServerFileInfo> getServerFileList(){
        ArrayList<ServerFileInfo> fileList = new ArrayList<>();
        try {
            URL url = new URL("http://"+getServerAddress()+"/filelist");
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setConnectTimeout(3000); // タイムアウト 3 秒
            connection.setReadTimeout(3000);
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(false);   // リダイレクト無効化

            connection.connect();

            int responseCode = connection.getResponseCode();
            String response = connection.getResponseMessage();
            String body = convertToString(connection.getInputStream());

            connection.disconnect();

            JSONObject json = new JSONObject(body);
            JSONArray array = json.getJSONArray("fileList");
            for(int i=0; i<array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                fileList.add(new ServerFileInfo(
                        obj.getString("path"),
                        obj.getLong("fileSize"),
                        obj.getLong("lastModified"),
                        obj.getBoolean("isDir")
                ));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Collections.sort(fileList, Comparator.comparing(a -> a.serverPath));
        return fileList;
    }

    public int sendDelete(ArrayList<String> serverPaths) {
        HttpURLConnection connection = null;
        try{
            URL url = new URL("http://"+getServerAddress()+"/filedelete");
            JSONArray jsonArray = new JSONArray();
            for (String path : serverPaths){
                jsonArray.put(path);
            }
            JSONObject json = new JSONObject();
            json.put("fileList", jsonArray);

            for(int retryCount =0; retryCount < RETRY_COUNT; retryCount++){
                connection = (HttpURLConnection)url.openConnection();
                connection.setConnectTimeout(3000); // タイムアウト 3 秒
                connection.setReadTimeout(3000);
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);       // body有効化
                connection.setInstanceFollowRedirects(false);   // リダイレクト無効化
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

                PrintStream ps = new PrintStream(connection.getOutputStream());
                ps.print(json);
                ps.close();
                connection.connect();

                // レスポンスコードの確認します。
                int responseCode = connection.getResponseCode();
                String response = connection.getResponseMessage();
                connection.disconnect();

                switch(responseCode){
                    case HttpURLConnection.HTTP_OK:
                        return ERROR_SUCCESS;

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
            return ERROR_FILE_OPEN;
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return ERROR_IO;
        }
        return ERROR_TIMEOUT;
    }

    private String convertToString(InputStream stream) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line = "";
        BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        try {
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }


}
