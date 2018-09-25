package internal;

import javax.crypto.spec.SecretKeySpec;
import de.upb.myannotation.Critical;


public class MySecretKeySpec  {


    public static SecretKeySpec generateFromFile() {
        FileStore fileStore = new FileStore();
        return new SecretKeySpec(fileStore.keys.getBytes(),"AES");
    }


    private static class FileStore {
        @Critical
        private String  keys= "waw";
    }

}
