package jp.ac.titech.itpro.sdl.androidfilesync;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.DhcpInfo;
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
import java.nio.charset.StandardCharsets;

public class ConnectServer {
    private final static String TAG = ConnectServer.class.getSimpleName();
    Context context;
    String password_digest;
    int port;
    InetAddress serverAddress;
    int local_port;

    public ConnectServer(Context context, String password_digest, int port){
        this.context = context;
        this.password_digest = password_digest;
        this.port = port;
    }

    public void connect(){
        serverAddress = null;
        String broadCastMessage = makeBroadCastMessage();
        sendBroadcast(broadCastMessage);
        receiveBroadCastResponse();
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

    String makeBroadCastMessage(){
        long unixTime = System.currentTimeMillis();
        String passWithTime = String.format("%d:%s", unixTime, password_digest);
        String sha256 = Encryption.sha256EncodeToString(passWithTime.getBytes(StandardCharsets.UTF_8));
        return String.format("%d:%s", unixTime, sha256);
    }

    void sendBroadcast(String messageStr) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try {
            DatagramSocket socket = new DatagramSocket();
            socket.setBroadcast(true);
            byte[] sendData = messageStr.getBytes();
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
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
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
            Socket socket = serverSocket.accept();

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String data = reader.readLine();
            Log.e(TAG, "Received data:"+data);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /*
    int receiveBroadCastResponse(){
        byte[] buffer = new byte[1000];
        DatagramSocket clientSocket = null;


        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiManager.MulticastLock lock = wifi.createMulticastLock("lock");
        lock.acquire();

        try {
            clientSocket = new DatagramSocket(port);
            clientSocket.setSoTimeout(3000);

            long startTime = System.currentTimeMillis();
            DatagramPacket packet = new DatagramPacket(buffer, 1000);

            while(startTime + 3000 > System.currentTimeMillis()){
                clientSocket.receive(packet);

                if(checkBroadCastResponse(buffer, packet.getLength())){
                    serverAddress = clientSocket.getInetAddress();
                    Log.i(TAG, "serverAddress:"+serverAddress);
                    return 0;
                }
            }

        } catch (SocketException e) {
            e.printStackTrace();
            return -1;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        finally {
            lock.release();
        }
        return -2;
    }*/

    boolean checkBroadCastResponse(byte[] data, int size){
        // todo チェックを書く
        return true;
    }
}
