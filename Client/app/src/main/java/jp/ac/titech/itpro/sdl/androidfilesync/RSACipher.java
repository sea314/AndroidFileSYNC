package jp.ac.titech.itpro.sdl.androidfilesync;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

// RSA暗号を扱うクラス
//
public class RSACipher {
    private static final int keySize = 2048;
    private static final String algorithmMode = "RSA/ECB/PKCS1Padding";

    private PublicKey publicKey;
    private PrivateKey privateKey;
    private Cipher encrypter;
    private Cipher decrypter;

    public void initialize() {
        clear();

        try{
            // 鍵ペア生成
            KeyPairGenerator keyGen;
            keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(keySize);
            KeyPair keyPair;
            keyPair = keyGen.generateKeyPair();
            publicKey = keyPair.getPublic();
            privateKey = keyPair.getPrivate();

            //　暗号アルゴリズム初期化
            encrypter = Cipher.getInstance(algorithmMode);
            encrypter.init(Cipher.ENCRYPT_MODE, publicKey);
            decrypter = Cipher.getInstance(algorithmMode);
            decrypter.init(Cipher.DECRYPT_MODE, privateKey);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
            // keySizeやalgorithmModeを変更しない限りこの例外は出ないはず
            e.printStackTrace();
            assert false;
        }
    }

    public void initialize(PublicKey publicKey) throws InvalidKeyException{
        clear();
        this.publicKey = publicKey;

        try {
            //　暗号アルゴリズム初期化
            encrypter = Cipher.getInstance(algorithmMode);
            encrypter.init(Cipher.ENCRYPT_MODE, publicKey);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            // keySizeやalgorithmModeを変更しない限りこの例外は出ないはず
            e.printStackTrace();
            assert false;
        }
    }

    // der形式のバイト列を公開鍵に変換して初期化
    public void initialize(byte[] publicKeyBytes) throws InvalidKeySpecException, InvalidKeyException {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory kf;
        try {
            kf = KeyFactory.getInstance("RSA");
            initialize(kf.generatePublic(keySpec));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            assert false;
        }
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    // 公開鍵をder形式に変換
    public byte[] getPublicKeyBytes(){
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKey.getEncoded());
        return keySpec.getEncoded();
    }

    public byte[] encrypt(byte[] bytes) throws IllegalBlockSizeException, BadPaddingException {
        if(encrypter != null){
            return encrypter.doFinal(bytes);
        }
        else{
            throw new IllegalStateException("初期化前もしくは秘密鍵を持たない状態で暗号化しようとしました。");
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
        publicKey = null;
        privateKey = null;
        encrypter = null;
        decrypter = null;
    }
}
