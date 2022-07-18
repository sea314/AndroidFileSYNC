package jp.ac.titech.itpro.sdl.androidfilesync;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;

public class SettingActivity extends AppCompatActivity {
    private final static String TAG = SettingActivity.class.getSimpleName();
    private final static String KEY_PORT = "KEY_PORT";
    private final static String KEY_PASSWORD = "KEY_PASSWORD";
    private EditText portEdit;
    private EditText passwordEdit;
    private ListView backupList;
    private Config config = new Config();

    ActivityResultLauncher<Intent> selectBackupDirectoryActivity = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent intent = result.getData();
                    ClipData clipData = intent.getClipData();
                    ArrayList<Uri> uris = new ArrayList<>();

                    if (clipData == null) {  // single selection
                        uris.add(intent.getData());
                    } else {  // multiple selection
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            uris.add(clipData.getItemAt(i).getUri());
                        }
                    }
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
    }

    public void onClickDeleteBackup(View v){
        Log.d(TAG, "onClickDeleteBackup in " + Thread.currentThread());
    }

    public void onClickOK(View v) {
        Log.d(TAG, "onClickOK in " + Thread.currentThread());
        config.port = Integer.parseInt(portEdit.getText().toString());

        String password = passwordEdit.getText().toString();
        if(!password.equals("****")){
            config.passwordDigest = Encryption.sha256EncodeToString(password);
        }
        finish();
    }

    public void onClickCancel(View v) {
        Log.d(TAG, "onClickCancel in " + Thread.currentThread());
        finish();
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
}