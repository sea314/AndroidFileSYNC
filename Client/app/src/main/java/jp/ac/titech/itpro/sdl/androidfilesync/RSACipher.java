package jp.ac.titech.itpro.sdl.androidfilesync;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class RSACipher {
    PublicKey publicKey;
    PrivateKey privateKey;
    Cipher encryptoCipher;
    Cipher decryptoCipher;

    public void initialize() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
        KeyPairGenerator keyGen = null;
        keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);

        KeyPair keyPair;
        keyPair = keyGen.generateKeyPair();
        publicKey = keyPair.getPublic();
        privateKey = keyPair.getPrivate();
        encryptoCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        encryptoCipher.init(Cipher.ENCRYPT_MODE, publicKey);
        decryptoCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        decryptoCipher.init(Cipher.DECRYPT_MODE, privateKey);
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public byte[] encrypt(byte[] bytes) throws IllegalBlockSizeException, BadPaddingException {
        return encryptoCipher.doFinal(bytes);
    }

    public byte[] decrypt(byte[] bytes) throws IllegalBlockSizeException, BadPaddingException {
        return decryptoCipher.doFinal(bytes);
    }
}
