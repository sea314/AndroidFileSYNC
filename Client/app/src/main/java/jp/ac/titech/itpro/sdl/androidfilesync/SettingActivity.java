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
    private final static int CONTEXT_MENU_BACKUP_ADD = 0;
    private final static int CONTEXT_MENU_BACKUP_DELETE = 1;
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
                    backupPathsAdapter.add(pathToViewPath(path));
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

        backupPaths.clear();
        for (String path : config.backupPaths){
            backupPaths.add(path);
            backupPathsAdapter.add(pathToViewPath(path));
        }

        if (savedInstanceState != null) {
            portEdit.setText(savedInstanceState.getString(KEY_PORT));
            passwordEdit.setText(savedInstanceState.getString(KEY_PASSWORD));
        }
        else{
            portEdit.setText(String.valueOf(config.port));

            if(config.passwordDigest.equals("")){
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
        config.port = Integer.parseInt(portEdit.getText().toString());

        String password = passwordEdit.getText().toString();
        if(!password.equals("****")){
            config.passwordDigest = Encryption.sha256EncodeToString(password);
        }
        config.backupPaths = new HashSet<>(backupPaths);
        config.Save(this);
        finish();
    }

    public void onClickCancel(View v) {
        Log.d(TAG, "onClickCancel in " + Thread.currentThread());
        finish();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        //コンテキストメニューの設定
        menu.add(0, CONTEXT_MENU_BACKUP_ADD, 0, "追加");
        menu.add(0, CONTEXT_MENU_BACKUP_DELETE, 0, "除去");
    }

    public boolean onContextItemSelected(MenuItem item) {

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId()) {
            case CONTEXT_MENU_BACKUP_ADD:
                onClickAddBackup(item.getActionView());
                return true;
            case CONTEXT_MENU_BACKUP_DELETE:
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

    private String pathToViewPath(String path){
        final String localStorage = "^/storage/emulated/0/";
        final String sdStorage = "^/storage/[0-9A-F]{4}-[0-9A-F]{4}/";
        if(path.matches(localStorage+".*")){
            return path.replaceFirst(localStorage, "ストレージ/");
        }
        if(path.matches(sdStorage+".*")){
            return path.replaceFirst(sdStorage, "SDカード/");
        }
        return path;
    }
}