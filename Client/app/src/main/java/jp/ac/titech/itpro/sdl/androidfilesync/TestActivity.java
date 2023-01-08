package jp.ac.titech.itpro.sdl.androidfilesync;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

public class TestActivity extends AppCompatActivity {
    private final static String TAG = TestActivity.class.getSimpleName();
    private EditText test_edit1, test_edit2, test_edit3;
    private TextView test_view1, test_view2, test_view3, test_view4, test_view5;

    public static void startTestActivity(Context context){
        Intent intent = new Intent(context, TestActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        test_edit1 = findViewById(R.id.test_edit1);
        test_edit2 = findViewById(R.id.test_edit2);
        test_edit3 = findViewById(R.id.test_edit3);
        test_view1 =  findViewById(R.id.test_view1);
        test_view2 =  findViewById(R.id.test_view2);
        test_view3 =  findViewById(R.id.test_view3);
        test_view4 =  findViewById(R.id.test_view4);
        test_view5 =  findViewById(R.id.test_view5);
    }


    public void onClickTest(View v){
        Log.d(TAG, "onClickTest");
        checkAES();
    }

    void checkAESKeySave(){
        try{
            AESCipher aesCipher1 = new AESCipher();
            AESCipher aesCipher2 = new AESCipher();
            aesCipher1.initialize();
            byte[] b = aesCipher1.getKeyBytes();
            aesCipher2.initialize(b);
            if(Arrays.equals(aesCipher1.getInitialVector(), aesCipher2.getInitialVector())){
                test_view1.setText("初期ベクトル一致");
            }
            else{
                test_view1.setText("初期ベクトル不一致");
            }
            if(aesCipher1.getSecretKey().equals(aesCipher2.getSecretKey())){
                test_view2.setText("鍵一致");
            }
            else{
                test_view2.setText("鍵不一致");
            }
        } catch (InvalidAlgorithmParameterException | InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    void checkRSAPublicKeySave(){
        try {
            RSACipher rsaCipher1 = new RSACipher();
            RSACipher rsaCipher2 = new RSACipher();
            rsaCipher1.initialize();
            byte[] b = rsaCipher1.getPublicKeyBytes();
            rsaCipher2.initialize(b);
            if(rsaCipher1.getPublicKey().equals(rsaCipher2.getPublicKey())){
                test_view1.setText("一致");
            }
            else{
                test_view1.setText("不一致");
            }
        } catch (InvalidKeySpecException | InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    void checkRSA(){
        // input: edit1
        // output: view1に暗号化データ、view2に復号化データ
        RSACipher rsaCipher1 = new RSACipher();
        RSACipher rsaCipher2 = new RSACipher();

        try {
            rsaCipher1.initialize();
            rsaCipher2.initialize(rsaCipher1.getPublicKey());

            String plain = test_edit1.getText().toString();

            byte[] encrypted = rsaCipher2.encrypt(plain.getBytes(StandardCharsets.UTF_8));

            test_view1.setText(encrypted.toString());

            byte[] decrypted = rsaCipher1.decrypt(encrypted);

            test_view2.setText(new String(decrypted));

        } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }
    }

    void checkAES(){
        // input: edit1
        // output: view1に暗号化データ、view2に復号化データ、view3に暗号化データ、view4に復号化データ
        AESCipher aesCipher1 = new AESCipher();
        AESCipher aesCipher2 = new AESCipher();

        try {
            aesCipher1.initialize();
            aesCipher2.initialize(aesCipher1.getInitialVector(), aesCipher1.getSecretKey());

            String plain = test_edit1.getText().toString();

            byte[] encrypted = aesCipher1.encrypt(plain.getBytes(StandardCharsets.UTF_8));

            test_view1.setText(encrypted.toString());

            byte[] decrypted = aesCipher2.decrypt(encrypted);

            test_view2.setText(new String(decrypted));

            // 暗号復号を逆にしたもの
            byte[] encrypted2 = aesCipher2.encrypt(plain.getBytes(StandardCharsets.UTF_8));

            test_view3.setText(encrypted2.toString());

            byte[] decrypted2 = aesCipher1.decrypt(encrypted2);

            test_view4.setText(new String(decrypted2));

        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
    }

}