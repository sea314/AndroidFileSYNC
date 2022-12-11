package jp.ac.titech.itpro.sdl.androidfilesync;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

// 共通鍵であるAES暗号を扱うクラス
// 初回以外のすべての暗号化に使う
public class AESCipher {
    private final int keySize = 256;
    private final String algorithmMode = "AES/CBC/PKCS5Padding";

    private SecretKey secretKey;
    private IvParameterSpec initialVector;

    private Cipher encrypter;
    private Cipher decrypter;

    public void initialize() {
        try {
            clear();
            // 共通鍵生成
            KeyGenerator keyGen = null;
            keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(keySize);
            secretKey = keyGen.generateKey();

            //　暗号アルゴリズム初期化
            encrypter = Cipher.getInstance(algorithmMode);
            encrypter.init(Cipher.ENCRYPT_MODE, secretKey);
            initialVector = new IvParameterSpec(encrypter.getIV());

            decrypter = Cipher.getInstance(algorithmMode);
            decrypter.init(Cipher.DECRYPT_MODE, secretKey, initialVector);
        } catch (NoSuchAlgorithmException e) {
            // keySizeやalgorithmModeを変更しない限りこの例外は出ないはず
            e.printStackTrace();
            assert false;
        } catch (NoSuchPaddingException e) {
            // keySizeやalgorithmModeを変更しない限りこの例外は出ないはず
            e.printStackTrace();
            assert false;
        } catch (InvalidAlgorithmParameterException e) {
            // keySizeやalgorithmModeを変更しない限りこの例外は出ないはず
            e.printStackTrace();
            assert false;
        } catch (InvalidKeyException e) {
            // keySizeやalgorithmModeを変更しない限りこの例外は出ないはず
            e.printStackTrace();
            assert false;
        }
    }

    public void initialize(SecretKey secretKey, IvParameterSpec initialVector) throws InvalidKeyException, InvalidAlgorithmParameterException {
        try {
            clear();
            this.secretKey = secretKey;
            this.initialVector = initialVector;

            encrypter = Cipher.getInstance(algorithmMode);
            encrypter.init(Cipher.ENCRYPT_MODE, secretKey, initialVector);

            decrypter = Cipher.getInstance(algorithmMode);
            decrypter.init(Cipher.DECRYPT_MODE, secretKey, initialVector);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException e) {
            // keySizeやalgorithmModeを変更しない限りこの例外は出ないはず
            e.printStackTrace();
            assert false;
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
            throw e;
        }
    }

    public SecretKey getSecretKey(){
        return secretKey;
    }

    public IvParameterSpec getInitialVector(){
        return initialVector;
    }

    public byte[] encrypt(byte[] bytes) throws IllegalBlockSizeException, BadPaddingException {
        if(encrypter != null){
            return encrypter.doFinal(bytes);
        }
        else{
            throw new IllegalStateException("初期化前に暗号化しようとしました。");
        }
    }

    public byte[] decrypt(byte[] bytes) throws IllegalBlockSizeException, BadPaddingException {
        if(decrypter != null){
            return decrypter.doFinal(bytes);
        }
        else{
            throw new IllegalStateException("初期化前に復号化しようとしました。");
        }
    }

    public void clear(){
        secretKey = null;
        initialVector = null;
        encrypter = null;
        decrypter = null;
    }
}
