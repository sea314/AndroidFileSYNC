package jp.ac.titech.itpro.sdl.androidfilesync;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class SettingActivity extends AppCompatActivity {
    private final static String TAG = SettingActivity.class.getSimpleName();
    private final static String KEY_PORT = "KEY_PORT";
    private final static String KEY_PASSWORD = "KEY_PASSWORD";
    private EditText portEdit;
    private EditText passwordEdit;
    private ListView backupList;
    private Config config = new Config();
    private ArrayList<String> backupPaths = new ArrayList<>();
    private ArrayAdapter backupPathsAdapter;

    ActivityResultLauncher<Intent> selectBackupDirectoryActivity = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent intent = result.getData();
                    Uri treeUri = intent.getData();
                    DocumentFile pickedDir = DocumentFile.fromTreeUri(this, treeUri);

                    String path = UriPath.getPathFromUri(this, pickedDir.getUri());

                    backupPaths.add(path);
                    backupPathsAdapter.add(localPathToServerPath(path));
                    Log.i(TAG, path);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        config.Load(getApplicationContext());

        portEdit = findViewById(R.id.port_edit);
        passwordEdit = findViewById(R.id.password_edit);
        backupList = findViewById(R.id.backup_list);

        // リスト項目とListViewを対応付けるArrayAdapterを用意する
        backupPathsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<String>());

        // ListViewにArrayAdapterを設定する
        backupList = (ListView)findViewById(R.id.backup_list);
        backupList.setAdapter(backupPathsAdapter);
        registerForContextMenu(backupList);

        backupPaths = config.getBackupPaths();
        for (String path : backupPaths){
            backupPathsAdapter.add(localPathToServerPath(path));
        }

        if (savedInstanceState != null) {
            portEdit.setText(savedInstanceState.getString(KEY_PORT));
            passwordEdit.setText(savedInstanceState.getString(KEY_PASSWORD));
        }
        else{
            portEdit.setText(String.valueOf(config.getPort()));

            if(config.getPasswordDigest().equals("")){
                passwordEdit.setText("");
            }
            else{
                passwordEdit.setText("****");
            }
        }
    }

    public void onClickAddBackup(View v){
        Log.d(TAG, "onClickAddBackup in " + Thread.currentThread());
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        selectBackupDirectoryActivity.launch(Intent.createChooser(intent, "バックアップ対象ディレクトリを指定"));
    }

    public void onClickOK(View v) {
        Log.d(TAG, "onClickOK in " + Thread.currentThread());
        config.setPort(Integer.parseInt(portEdit.getText().toString()));

        String password = passwordEdit.getText().toString();
        if(!password.equals("****")){
            config.setPasswordDigest(Encryption.sha256EncodeToString(password));
        }
        config.setBackupPaths(backupPaths);
        config.Save(this);
        finish();
    }

    public void onClickCancel(View v) {
        Log.d(TAG, "onClickCancel in " + Thread.currentThread());
        finish();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.context_menu, menu);
    }

    public boolean onContextItemSelected(MenuItem item) {

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId()) {
            case R.id.add_backup:
                onClickAddBackup(item.getActionView());
                return true;
            case  R.id.delete_backup:
                backupPaths.remove(info.position);
                backupPathsAdapter.remove(backupPathsAdapter.getItem(info.position));
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_PORT, portEdit.getText().toString());
        outState.putString(KEY_PASSWORD, passwordEdit.getText().toString());
    }

    public static void startSettingActivity(Context context){
        Intent intent = new Intent(context, SettingActivity.class);
        context.startActivity(intent);
    }

    static String localPathToServerPath(String localPath){
        return LocalFileInfo.localPathToServerPath(localPath);
    }
}