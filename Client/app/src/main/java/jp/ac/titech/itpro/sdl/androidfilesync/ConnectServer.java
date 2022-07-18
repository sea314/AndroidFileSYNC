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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
    public final static int ERROR_FILE_OPEN = -4;

    public final static int MODE_DESKTOP = 0;
    public final static int MODE_BACKUP = 1;

    final static int BUFFER_SIZE = 1024*1024;
    final static int RETRY_COUNT = 2;   // 送信のリトライ回数

    class FileInfo{
        public String path;
        public long fileSize;
        public long lastModified;
        public boolean isDir;
        public FileInfo(String path, long fileSize, long lastModified, boolean idDir){
            this.path = path;
            this.fileSize = fileSize;
            this.lastModified = lastModified;
            this.isDir = idDir;
        }
    }

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

    public int sendFile(String path, int mode){
        byte[] fileBuffer = new byte[BUFFER_SIZE];
        int bufferSize = 0;
        FileInputStream fileSteam = null;
        try {
            URL url = new URL("http://"+getAddress()+"/file");
            File file = new File(path);
            fileSteam = new FileInputStream(file);

            int splitIndex = 0;
            for(; (bufferSize = fileSteam.read(fileBuffer)) > 0; splitIndex++) {
                sendFileData(splitIndex, url, fileBuffer,
                        bufferSize, file.length(), path,
                        file.lastModified(), mode);
            }
            sendFileData(splitIndex, url, fileBuffer,
                    0, 0, path,
                    file.lastModified(), mode);

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

    public int backup(ArrayList<String> paths){

        ArrayList<FileInfo> serverFileList = getServerFileList();
        ArrayList<FileInfo> localFileList = getLocalFileList(paths);

        Log.i(TAG, localFileList.get(0).path);
        Log.i(TAG, ""+localFileList.get(0).fileSize);
        Log.i(TAG, ""+localFileList.get(0).lastModified);
        Log.i(TAG, ""+localFileList.get(0).isDir);
        return  ERROR_SUCCESS;
    }

    private ArrayList<FileInfo> getLocalFileList(ArrayList<String> paths){
        ArrayList<FileInfo> fileList = new ArrayList<>();

        for(String path : paths){
            File file = new File(path);
            File[] list = file.listFiles();

            if (list != null) {
                for (File a : list) {
                    fileList.add(new FileInfo(
                            a.getPath(),
                            a.length(),
                            a.lastModified(),
                            a.isDirectory()
                    ));
                }
            }
        }
        return fileList;
    }


    private ArrayList<FileInfo> getServerFileList(){
        ArrayList<FileInfo> fileList = new ArrayList<>();
        try {
            URL url = new URL("http://"+getAddress()+"/filelist");
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
            Log.i(TAG, responseCode+":"+response);
            Log.i(TAG, "body:"+body);

            JSONObject json = new JSONObject(body);
            JSONArray array = json.getJSONArray("fileList");
            for(int i=0; i<array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                fileList.add(new FileInfo(
                        obj.getString("path"),
                        obj.getLong("fileSize"),
                        obj.getLong("lastModified"),
                        obj.getBoolean("isDir")
                ));
                Log.i(TAG, array.get(i).toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return fileList;
    }

    private String convertToString(InputStream stream) throws IOException {
        StringBuffer sb = new StringBuffer();
        String line = "";
        BufferedReader br = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
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
