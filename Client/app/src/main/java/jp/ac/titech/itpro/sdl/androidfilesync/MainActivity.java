package jp.ac.titech.itpro.sdl.androidfilesync;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();

    private Config config = new Config();

    String necessaryPermissions[] = {
        Manifest.permission.INTERNET,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_WIFI_STATE,
    };

    ActivityResultLauncher<Intent> selectFileActivity = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent intent = result.getData();
                    if(intent != null){
                        ClipData clipData = intent.getClipData();
                        ArrayList<Uri> uris = new ArrayList<>();

                        if (clipData == null) {  // single selection
                            uris.add(intent.getData());
                        } else {  // multiple selection
                            for (int i = 0; i < clipData.getItemCount(); i++) {
                                uris.add(clipData.getItemAt(i).getUri());
                            }
                        }

                        startSendFileService(uris, config.getPort(), config.getPasswordDigest());
                    }
                }
            });



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        config.Load(getApplicationContext());

        Intent intent = getIntent();
        String action = intent.getAction();

        if(checkNecessaryPermissions() == PackageManager.PERMISSION_GRANTED){
            if (Intent.ACTION_SEND.equals(action)) {    // 共有(単一ファイル)
                Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                startSendFileService(
                        new ArrayList<>(Arrays.asList(uri)),
                        config.getPort(),
                        config.getPasswordDigest()
                );
            } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {        // 共有(複数ファイル)
                ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                startSendFileService(uris, config.getPort(), config.getPasswordDigest());
            }
        }
        else{
            ActivityCompat.requestPermissions(this, necessaryPermissions, 1);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        config.Load(this);
    }

    public void onClickSendFile(View v){
        Log.d(TAG, "onClickSendFile in " + Thread.currentThread());
        if(checkNecessaryPermissions() == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

            selectFileActivity.launch(Intent.createChooser(intent, "送信するファイルを指定"));
        }
        else{
            ActivityCompat.requestPermissions(this, necessaryPermissions, 1);
        }
    }

    public void onClickBackup(View v){
        Log.d(TAG, "onClickBackup in " + Thread.currentThread());
        if(checkNecessaryPermissions() == PackageManager.PERMISSION_GRANTED) {
            SendFileService.startActionBackup(this, config.getBackupPaths(), config.getPort(), config.getPasswordDigest());
        }
        else{
            ActivityCompat.requestPermissions(this, necessaryPermissions, 1);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu");
        getMenuInflater().inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected");
        switch (item.getItemId()) {
            case R.id.menu_option:
                SettingActivity.startSettingActivity(this);
                break;
        }
        invalidateOptionsMenu();
        return super.onOptionsItemSelected(item);
    }

    void startSendFileService(ArrayList<Uri> uris, int port, String passwordDigest){
        ArrayList<String> paths = new ArrayList<>();
        for(Uri uri : uris){
            String path = UriPath.getPathFromUri(this, uri);
            if(path == null){
                Log.e(TAG, "uriをファイル名に変換できません:\""+uri+"\"");
                continue;
            }
            paths.add(path);
        }

        SendFileService.startActionSendFile(this, paths, port, passwordDigest);
    }

    int checkNecessaryPermissions(){
        int result[] = new int[necessaryPermissions.length];

        for(int i=0; i<necessaryPermissions.length; i++){
            result[i] = checkSelfPermission(necessaryPermissions[i]);
            Log.i(TAG, necessaryPermissions[i]+":"+checkSelfPermission(necessaryPermissions[i]));
        }

        return Arrays.stream(result).allMatch(a -> a==PackageManager.PERMISSION_GRANTED) ? PackageManager.PERMISSION_GRANTED : PackageManager.PERMISSION_DENIED;
    }

}
