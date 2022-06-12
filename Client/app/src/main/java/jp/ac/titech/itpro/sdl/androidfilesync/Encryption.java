package jp.ac.titech.itpro.sdl.androidfilesync;

import android.util.Base64;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Encryption {
    // sha256ハッシュ値を計算
    public static String sha256EncodeToString(byte[] data, int bufferSize){
        try {
            byte[] cipher_byte;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(data, 0, bufferSize);
            cipher_byte = md.digest();
            return Base64.encodeToString(cipher_byte, Base64.DEFAULT);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
    public static String sha256EncodeToString(byte[] data){
        return sha256EncodeToString(data, data.length);
    }

    public static byte[] base64Encode(byte[] bytes, int size){
        return Base64.encode(Arrays.copyOf(bytes, size), Base64.DEFAULT);
    }

    public static byte[] base64Decode(byte[] bytes){
        return Base64.encode(bytes, Base64.DEFAULT);
    }

}
