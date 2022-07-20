package jp.ac.titech.itpro.sdl.androidfilesync;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class SendFileService extends IntentService {
    private final static String TAG = SendFileService.class.getSimpleName();
    private static final String ACTION_SEND_FILE = "jp.ac.titech.itpro.sdl.androidfilesync.action.SEND_FILE";
    private static final String ACTION_BACKUP = "jp.ac.titech.itpro.sdl.androidfilesync.action.BACKUP";

    private static final String EXTRA_PATHS = "jp.ac.titech.itpro.sdl.androidfilesync.extra.PATHS";
    private static final String EXTRA_PASSWORD_DIGEST = "jp.ac.titech.itpro.sdl.androidfilesync.extra.PASSWORD_DIGEST";
    private static final String EXTRA_PORT = "jp.ac.titech.itpro.sdl.androidfilesync.extra.PORT";

    private static MutableLiveData<Boolean> isRunning = new MutableLiveData<>(false);
    private static MutableLiveData<ProgressBarInfo> progressInfo = new MutableLiveData<>();

    public SendFileService() {
        super(TAG);
    }

    public static void startActionSendFile(Context context, ArrayList<String> paths, int port, String passwordDigest) {
        if(isRunning.getValue()){
            return;
        }
        isRunning.postValue(true);
        progressInfo.postValue(new ProgressBarInfo(0, 0, "初期化中"));
        Intent intent = new Intent(context, SendFileService.class);
        intent.setAction(ACTION_SEND_FILE);
        intent.putStringArrayListExtra(EXTRA_PATHS, paths);
        intent.putExtra(EXTRA_PASSWORD_DIGEST, passwordDigest);
        intent.putExtra(EXTRA_PORT, port);
        context.startService(intent);
    }

    public static void startActionBackup(Context context, ArrayList<String> backupPaths, int port, String passwordDigest) {
        if(isRunning.getValue()){
            return;
        }
        isRunning.postValue(true);
        progressInfo = new MutableLiveData<>();
        progressInfo.postValue(new ProgressBarInfo(0, 0, "初期化中"));
        Intent intent = new Intent(context, SendFileService.class);
        intent.setAction(ACTION_BACKUP);
        intent.putStringArrayListExtra(EXTRA_PATHS, backupPaths);
        intent.putExtra(EXTRA_PASSWORD_DIGEST, passwordDigest);
        intent.putExtra(EXTRA_PORT, port);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent in " + Thread.currentThread());
        Context context = getApplicationContext();

        if(intent == null){
            return;
        }

        String action = intent.getAction();
        ArrayList<String> paths = intent.getStringArrayListExtra(EXTRA_PATHS);
        String password_digest = intent.getStringExtra(EXTRA_PASSWORD_DIGEST);
        int port = intent.getIntExtra(EXTRA_PORT, 0);

        ConnectServer connection = new ConnectServer(context, password_digest, port);
        int connErr = connection.connect();

        if(connErr != ConnectServer.ERROR_SUCCESS){
            Log.e(TAG, "サーバー接続失敗");
            switch (connErr){
                case ConnectServer.ERROR_AUTHORIZATION:
                    progressInfo.postValue(new ProgressBarInfo(0, 0, "サーバー接続失敗：認証失敗"));
                    break;
                case ConnectServer.ERROR_TIMEOUT:
                    progressInfo.postValue(new ProgressBarInfo(0, 0, "サーバー接続失敗：タイムアウトしました"));
                    // todo:broadcastで通知
                    break;
                case ConnectServer.ERROR_IO:
                    progressInfo.postValue(new ProgressBarInfo(0, 0, "サーバー接続失敗：IOエラー"));
                    break;
            }
            return;
        }

        if (ACTION_BACKUP.equals(action)) {
            onActionBackup(connection, paths);
        }
        else if(ACTION_SEND_FILE.equals(action)){
            onActionSendFile(connection, paths);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate in " + Thread.currentThread());
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy in " + Thread.currentThread());
        isRunning.postValue(false);
        super.onDestroy();
    }

    private void onActionSendFile(ConnectServer connection, ArrayList<String> localPaths){
        long totalSize = 0;
        ArrayList<Long> sizes = new ArrayList<>();
        for(String localPath : localPaths){
            File file = new File(localPath);
            sizes.add(file.length());
            totalSize += file.length();
        }

        long sentSize = 0;
        for(int i=0; i<localPaths.size(); i++) {
            String localPath = localPaths.get(i);
            String serverPath =  localPathToServerPath(localPath);
            Log.i(TAG, "send file:"+serverPath);
            progressInfo.postValue(new ProgressBarInfo(
                    (float)sentSize/totalSize,
                    (float)(sentSize+sizes.get(i))/totalSize,
                    "送信中："+serverPath));
            connection.sendFile(localPath, localPathToServerPath(localPath), ConnectServer.MODE_DESKTOP);
            sentSize += sizes.get(i);
        }
        progressInfo.postValue(new ProgressBarInfo(100,100, "送信完了"));
    }

    private void onActionBackup(ConnectServer connection, ArrayList<String> paths){
        Log.i(TAG, "get server file list");
        progressInfo.postValue(new ProgressBarInfo(0,0, "サーバーのファイルリスト取得中"));
        ArrayList<ServerFileInfo> serverFileList = connection.getServerFileList();
        Log.i(TAG, "list up local file");
        progressInfo.postValue(new ProgressBarInfo(0,0, "端末のファイルリスト取得中"));
        ArrayList<LocalFileInfo> localFileList = getLocalFileList(paths);
        ArrayList<ServerFileInfo> deleteFileList = new ArrayList<>();
        ArrayList<LocalFileInfo> sendFileList = new ArrayList<>();

        Log.i(TAG, "list up sync file");
        progressInfo.postValue(new ProgressBarInfo(0,0, "同期するファイルリスト取得中"));

        int serverIndex = 0;
        int localIndex = 0;

        while(serverIndex < serverFileList.size() && localIndex < localFileList.size()){
            ServerFileInfo s = serverFileList.get(serverIndex);
            LocalFileInfo l = localFileList.get(localIndex);

            if(s.approximatelyEqual(l)){
                serverIndex++;
                localIndex++;
                continue;
            }

            int cmp = s.serverPath.compareTo(l.serverPath);

            if(cmp == 0){   // 名前一致 → 更新日時が違うorフォルダファイルの違い
                sendFileList.add(l);
                deleteFileList.add(s);
                serverIndex++;
                localIndex++;
            }
            else if(cmp > 0){   // s.path > l.path
                sendFileList.add(l);
                localIndex++;
            }
            else{   // s.path < l.path
                if(!l.serverPath.startsWith(s.serverPath+"/")){
                    deleteFileList.add(s);
                }
                serverIndex++;
            }
        }
        if(serverIndex < serverFileList.size()){
            deleteFileList.addAll(serverFileList.subList(serverIndex, serverFileList.size()));
        }
        if(localIndex < localFileList.size()){
            sendFileList.addAll(localFileList.subList(localIndex, localFileList.size()));
        }

        sendFileList.removeIf(a -> a.isDir);
        for(int i=0;i<deleteFileList.size();i++){
            ServerFileInfo file = deleteFileList.get(i);
            if(file.isDir){
                deleteFileList.removeIf(a -> a.serverPath.startsWith(file.serverPath+"/"));
            }
        }

        Log.i(TAG, "delete file");
        ArrayList<String> deleteFiles = new ArrayList<>();
        for (ServerFileInfo a : deleteFileList) {
            deleteFiles.add(a.serverPath);
        }
        connection.sendDelete(deleteFiles);

        long totalSize = sendFileList.stream().mapToLong(a->a.fileSize).sum();
        long sentSize = 0;
        for (LocalFileInfo a : sendFileList) {
            progressInfo.postValue(new ProgressBarInfo(
                    (float)sentSize/totalSize,
                    (float)(sentSize+a.fileSize)/totalSize,
                    "送信中："+a.serverPath));
            Log.i(TAG, "send file:"+a.serverPath);
            connection.sendFile(a.localPath, a.serverPath, ConnectServer.MODE_BACKUP);
            sentSize += a.fileSize;
        }
        progressInfo.postValue(new ProgressBarInfo(100,100, "送信完了"));
    }

    private ArrayList<LocalFileInfo> getLocalFileList(ArrayList<String> paths){
        ArrayList<LocalFileInfo> fileList = new ArrayList<>();

        class FileListUp{
            public void ListUp(ArrayList<LocalFileInfo> fileList, String path){
                File file = new File(path);
                File[] list = file.listFiles();
                if (list != null) {
                    for (File a : list) {
                        if(a.isDirectory()){
                            ListUp(fileList, a.getPath());
                        }
                        else{
                            fileList.add(new LocalFileInfo(
                                    a.getPath(),
                                    a.length(),
                                    a.lastModified(),
                                    false
                            ));
                        }
                    }
                }
            }
        }

        FileListUp listUp = new FileListUp();

        for(String path : paths){
            listUp.ListUp(fileList, path);
        }
        Collections.sort(fileList, Comparator.comparing(a -> a.serverPath));
        return fileList;
    }

    private static String localPathToServerPath(String localPath){
        return LocalFileInfo.localPathToServerPath(localPath);
    }

    public static LiveData<ProgressBarInfo> getProgressInfo(){
        return progressInfo;
    }

    public static LiveData<Boolean> IsRunning(){
        return isRunning;
    }
}
