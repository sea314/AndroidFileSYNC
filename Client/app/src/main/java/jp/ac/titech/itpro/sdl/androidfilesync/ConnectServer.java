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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

public class ConnectServer {
    private final static String TAG = ConnectServer.class.getSimpleName();
    Context context;
    String password_digest;
    String msg_digest;
    int port;
    InetAddress serverAddress;
    int local_port;

    public ConnectServer(Context context, String password_digest, int port){
        this.context = context;
        this.password_digest = password_digest;
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
        String passWithTime = String.format("%d:%s", unixTime, password_digest);
        msg_digest = Encryption.sha256EncodeToString(passWithTime.getBytes(StandardCharsets.UTF_8));
        String messageStr =  String.format("%d:%s", unixTime, msg_digest);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try {
            DatagramSocket socket = new DatagramSocket();
            socket.setBroadcast(true);
            byte[] sendData = messageStr.getBytes(StandardCharsets.UTF_8);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, getBroadcastAddress(context), 12345);
            socket.send(sendPacket);
            local_port = socket.getLocalPort();
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
            ServerSocket serverSocket = new ServerSocket(local_port);
            serverSocket.setSoTimeout(3000);
            Socket socket = serverSocket.accept();

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String data = reader.readLine();

            if(!data.equals(Encryption.sha256EncodeToString(msg_digest+password_digest))){
                Log.e(TAG, "auth error");
                msg_digest = null;

                return -1;
            }

            serverAddress = socket.getInetAddress();
        } catch (SocketTimeoutException e){
            e.printStackTrace();
            Log.i(TAG, "receiveBroadCastResponse: タイムアウト");
            return -2;
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(TAG, "receiveBroadCastResponse: IOエラー");
            return -3;
        }
        return 0;
    }
}
