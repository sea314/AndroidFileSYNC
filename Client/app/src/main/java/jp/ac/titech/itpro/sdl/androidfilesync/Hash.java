package jp.ac.titech.itpro.sdl.androidfilesync;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Hash {
    // sha256ハッシュ値を計算
    public static byte[] sha256Encode(byte[] data, int size){
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(data, 0, size);
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] sha256Encode(byte[] data){
        return sha256Encode(data, data.length);
    }
}
