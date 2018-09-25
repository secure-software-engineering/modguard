package api;


import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.KeySpec;
import internal.MySecretKeySpec;
import java.net.*;

/**
 * Created by adann on 03.11.17.
 */
public class SecretKeyFactory {


   public static DatagramSocket getMyCustomChiper() throws Exception {
        DatagramSocket key = MySecretKeySpec.generateFromFile();
       return key;
	//return null;
    }
}

