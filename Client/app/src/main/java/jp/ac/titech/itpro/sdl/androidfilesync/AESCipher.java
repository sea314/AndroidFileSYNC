package jp.ac.titech.itpro.sdl.androidfilesync;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
import javax.crypto.spec.SecretKeySpec;

// 共通鍵であるAES暗号を扱うクラス
// 初回以外のすべての暗号化に使う
public class AESCipher {
    private final int keySize = 256;
    private final String algorithmMode = "AES/CBC/PKCS5Padding";    // 実質的にpkcs7

    private SecretKey secretKey;
    private byte[] initialVector;

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
            initialVector = encrypter.getIV();
            decrypter = Cipher.getInstance(algorithmMode);
            decrypter.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(initialVector));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | InvalidKeyException e) {
            // keySizeやalgorithmModeを変更しない限りこの例外は出ないはず
            e.printStackTrace();
            assert false;
        }
    }

    public void initialize(byte[] initialVector, SecretKey secretKey) throws InvalidKeyException, InvalidAlgorithmParameterException {
        try {
            clear();
            this.secretKey = secretKey;
            this.initialVector = initialVector;

            encrypter = Cipher.getInstance(algorithmMode);
            encrypter.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(initialVector));

            decrypter = Cipher.getInstance(algorithmMode);
            decrypter.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(initialVector));
        } catch (NoSuchPaddingException | NoSuchAlgorithmException e) {
            // keySizeやalgorithmModeを変更しない限りこの例外は出ないはず
            e.printStackTrace();
            assert false;
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void initialize(byte[] bytes) throws InvalidAlgorithmParameterException, InvalidKeyException {
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        int ivSize = buf.getInt();
        byte[] iv = new byte[ivSize];
        buf.get(iv);
        int keySize = buf.getInt();
        byte[] keyBytes = new byte[keySize];
        buf.get(keyBytes);
        SecretKey key = new SecretKeySpec(keyBytes, "AES");
        initialize(iv, key);
    }

    public SecretKey getSecretKey(){
        return secretKey;
    }

    public byte[] getInitialVector(){
        return initialVector;
    }

    public byte[] getKeyBytes(){
        byte[] iv = initialVector;
        byte[] key = secretKey.getEncoded();
        ByteBuffer buf = ByteBuffer.allocate(iv.length+key.length+8).order(ByteOrder.BIG_ENDIAN);
        buf.putInt(iv.length);
        buf.put(iv);
        buf.putInt(key.length);
        buf.put(key);
        return buf.array();
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
