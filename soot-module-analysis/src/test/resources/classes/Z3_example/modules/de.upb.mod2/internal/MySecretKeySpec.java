package internal;

import javax.crypto.spec.SecretKeySpec;
import de.upb.myannotation.Critical;


public class MySecretKeySpec extends SecretKeySpec {

    private static FileStore fileStore = new FileStore();

    private MySecretKeySpec(String keyFromFile) {
        super(keyFromFile.getBytes(), "AES");
    }


    public static MySecretKeySpec generateFromFile() {
        String key = fileStore.getNext();
        return new MySecretKeySpec(key);
    }


    private static class FileStore {
        @Critical
        private String keys;
        private int usedKey = 0;

        private FileStore() {
            parseFile();
        }

        private void parseFile() {
           // List<String> readKeys = Files.readAllLines("myFile", Charset.defaultCharset());
           // keys = (String[]) readKeys.toArray();
            this.keys = "hihi";
        }

        protected String getNext() {
           // if (usedKey < keys.length)
             //   return keys[usedKey++];
                
                return this.keys;

        }


    }

}
