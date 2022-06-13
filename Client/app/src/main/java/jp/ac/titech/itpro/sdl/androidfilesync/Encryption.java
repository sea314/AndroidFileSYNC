package jp.ac.titech.itpro.sdl.androidfilesync;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
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
            return Base64.encodeToString(cipher_byte, Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String sha256EncodeToString(byte[] data){
        return sha256EncodeToString(data, data.length);
    }

    public static String sha256EncodeToString(String data){
        return sha256EncodeToString(data.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] base64Encode(byte[] data, int size){
        return Base64.encode(Arrays.copyOf(data, size), Base64.NO_WRAP);
    }

    public static byte[] base64Decode(byte[] bytes){
        return Base64.encode(bytes, Base64.NO_WRAP);
    }

}
