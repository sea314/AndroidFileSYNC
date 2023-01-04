package jp.ac.titech.itpro.sdl.androidfilesync;

import static android.content.Context.WIFI_SERVICE;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.StrictMode;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
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
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

public class ConnectServer {
    private final static String TAG = ConnectServer.class.getSimpleName();
    static final int randomBytesSize = 8;
    Context context;
    String pwdDigestBase64;
    String clientMsgDigestBase64;
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
    public final static int ERROR_PARSE = -5;
    public final static int ERROR_VERSION = -6;
    public final static int ERROR_KEY = -7;

    public final static int MODE_DESKTOP = 0;
    public final static int MODE_BACKUP = 1;

    final static int BUFFER_SIZE = 1024*1024;
    final static int RETRY_COUNT = 2;   // 送信のリトライ回数


    public ConnectServer(Context context, String passwordDigest, int port){
        this.context = context;
        this.pwdDigestBase64 = passwordDigest;
        this.port = port;
    }

    public int connect(){
        serverAddress = null;
        sendBroadcast();
        int response = receiveBroadCastResponse();
        if(response != ERROR_SUCCESS)   return response;
        return login();
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
                ipAddr&0xff, (ipAddr>>8)&0xff, (ipAddr>>16)&0xff, (ipAddr>>24)&0xff);
    }

    void sendBroadcast() {
        SecureRandom random = new SecureRandom();
        byte[] randomBytes = new byte[randomBytesSize];

        random.nextBytes(randomBytes);
        String randomBytesBase64 = Base64.encodeToString(randomBytes, Base64.URL_SAFE | Base64.NO_WRAP);
        clientMsgDigestBase64 = makeMessageHash(Collections.singletonList(randomBytes), getWifiIPAddress(context), pwdDigestBase64);

        // メッセージ本体　(アプリ名),(バージョン),(base64乱数),(base64公開鍵),(base64メッセージハッシュ)
        String messageStr =  String.format("FileSYNC,0.1,%s,%s", randomBytesBase64, clientMsgDigestBase64);

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
            serverAddress = socket.getInetAddress();

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String data = reader.readLine();

            String[] strs = data.split(",");
            if(strs.length < 3 || !strs[0].equals("FileSYNC")){
                Log.w(TAG, "receiveBroadCastResponse: parse error");
                return ERROR_PARSE;
            }

            // バージョン
            switch (strs[1]){
                case "0.1":
                    // メッセージ本体　(アプリ名),(バージョン),(base64公開鍵),(メッセージbase64ハッシュ)
                    if(strs.length != 4){
                        Log.w(TAG, "receiveBroadCastResponse: parse error");
                        return ERROR_PARSE;
                    }
                    byte[] publicKeyBytes = Base64.decode(strs[2], Base64.URL_SAFE | Base64.NO_WRAP);

                    String hostAddrStr = serverAddress.getHostAddress();

                    // ハッシュ値計算　clientMsgDiges+pwdDigest+publicKeyBytes
                    String serverMsgDigestBase64 = makeMessageHash(Arrays.asList(
                            clientMsgDigestBase64.getBytes(StandardCharsets.UTF_8),
                            publicKeyBytes
                    ), hostAddrStr, pwdDigestBase64);

                    if(!strs[3].equals(serverMsgDigestBase64)){
                        Log.w(TAG, "receiveBroadCastResponse: メッセージダイジェスト不一致");
                        return ERROR_AUTHORIZATION;
                    }

                    rsaCipher = new RSACipher();
                    rsaCipher.initialize(publicKeyBytes);

                    break;

                default:
                    return ERROR_VERSION;
            }
            serverSocket.close();
        } catch (SocketTimeoutException e){
            e.printStackTrace();
            Log.e(TAG, "receiveBroadCastResponse: タイムアウト");
            return ERROR_TIMEOUT;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "receiveBroadCastResponse: IOエラー");
            return ERROR_IO;
        } catch (InvalidKeySpecException | InvalidKeyException e) {
            e.printStackTrace();
            Log.e(TAG, "receiveBroadCastResponse: 無効な公開鍵");
            return ERROR_KEY;
        }
        Log.i(TAG, "server address:"+getServerAddress());
        return ERROR_SUCCESS;
    }

    int login() {
        try {
            aesCipher = new AESCipher();
            aesCipher.initialize();
            byte[] key = aesCipher.getKeyBytes();
            byte[] encryptedKey = rsaCipher.encrypt(key);

            // 公開鍵暗号化した共通鍵、(共通鍵、IPアドレス、パスワード)のハッシュを送る
            URL url = new URL("http://" + getServerAddress() + "/login");
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
//            connection.setConnectTimeout(3000); // タイムアウト 3 秒
//            connection.setReadTimeout(3000);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);       // body有効化
            connection.setInstanceFollowRedirects(false);   // リダイレクト無効化
            connection.setRequestProperty("Content-Type", "application/octet-stream");  // バイナリ全般
            connection.setRequestProperty("#Sha-256", makeMessageHash(Collections.singletonList(key), getWifiIPAddress(context), pwdDigestBase64));

            connection.setFixedLengthStreamingMode(encryptedKey.length);
            OutputStream httpStream = connection.getOutputStream();
            httpStream.write(encryptedKey, 0, encryptedKey.length);

            connection.connect();

            // レスポンスコードの確認します。
            int responseCode = connection.getResponseCode();
            String response = connection.getResponseMessage();

            switch(responseCode){
                case HttpURLConnection.HTTP_OK:
                    return ERROR_SUCCESS;

                default:
                    Log.e(TAG, responseCode+":"+response);
                    return ERROR_AUTHORIZATION;
            }

        } catch (IOException | IllegalBlockSizeException e) {
            e.printStackTrace();
            return ERROR_AUTHORIZATION;
        }
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
                connection.setRequestProperty("Sha-256", Hash.sha256EncodeBase64(fileBuffer, bufferSize));
                switch(mode){
                    case MODE_DESKTOP:
                        connection.setRequestProperty("Mode", "DESKTOP");
                        break;
                    case MODE_BACKUP:
                        connection.setRequestProperty("Mode", "BACKUP");
                        break;
                }
                connection.setFixedLengthStreamingMode(bufferSize);
                OutputStream httpStream = connection.getOutputStream();

                httpStream.write(fileBuffer, 0, bufferSize);
                connection.connect();

                // レスポンスコードの確認します。
                int responseCode = connection.getResponseCode();
                String response = connection.getResponseMessage();

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

    private boolean veryfyMessage(String msgDigestBase64, List<byte[]> msgs){
        return msgDigestBase64.equals(makeMessageHash(msgs, getWifiIPAddress(context), pwdDigestBase64));
    }

    private static String makeMessageHash(List<byte[]> msgs, String ipAddr, String pwdDigestBase64) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try{
            for(byte[] msg : msgs){
                buf.write(msg);
            }
            buf.write(ipAddr.getBytes(StandardCharsets.UTF_8));
            buf.write(pwdDigestBase64.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return Hash.sha256EncodeBase64(buf.toByteArray());
    }

    private void httpEncoder(HttpURLConnection connection, byte[] body, ArrayList<Pair<String, byte[]>> requests){
        try {
            connection.setInstanceFollowRedirects(false);   // リダイレクト無効化
            connection.setRequestProperty("Content-Type", "application/octet-stream");  // バイナリ全般

            // ハッシュ値計算用バッファ
            ByteArrayOutputStream buf = new ByteArrayOutputStream();

            for(Pair<String, byte[]> req : requests){
                buf.write(req.first.getBytes(StandardCharsets.UTF_8));
                buf.write(req.second);
                byte[] keyEncrypted = aesCipher.encrypt(req.first.getBytes(StandardCharsets.UTF_8));
                byte[] reqEncrypted = aesCipher.encrypt(req.second);
                String keyBase64 = Base64.encodeToString(keyEncrypted, Base64.URL_SAFE | Base64.NO_WRAP);
                String reqBase64 = Base64.encodeToString(reqEncrypted, Base64.URL_SAFE | Base64.NO_WRAP);
                connection.setRequestProperty(keyBase64, reqBase64);
            }

            if(body != null) {
                byte[] bodyEncrypted = aesCipher.encrypt(body);
                buf.write(body);

                connection.setRequestMethod("POST");
                connection.setDoOutput(true);       // body有効化
                connection.setFixedLengthStreamingMode(bodyEncrypted.length);
                OutputStream httpStream = connection.getOutputStream();
                httpStream.write(bodyEncrypted, 0, bodyEncrypted.length);
                connection.setRequestProperty("#Sha-256", makeMessageHash(Arrays.asList(buf.toByteArray(), body), getWifiIPAddress(context), pwdDigestBase64));
            }
            else{
                connection.setRequestMethod("GET");
                connection.setDoOutput(false);       // body無効化
                connection.setRequestProperty("#Sha-256", makeMessageHash(Collections.singletonList(buf.toByteArray()), getWifiIPAddress(context), pwdDigestBase64));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
