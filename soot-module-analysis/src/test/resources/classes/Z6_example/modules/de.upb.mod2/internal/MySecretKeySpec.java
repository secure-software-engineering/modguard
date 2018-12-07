package internal;

import javax.crypto.spec.SecretKeySpec;
import de.upb.myannotation.Critical;
import java.net.*;

public class MySecretKeySpec  {


    public static DatagramSocket generateFromFile() throws Exception {
        FileStore fileStore = new FileStore();
        return new DatagramSocket(80,fileStore.keys);
    }


    private static class FileStore {
        @Critical
        private InetAddress keys;

		public FileStore() throws Exception{
			keys =  InetAddress.getByName("127.0.0.1");
		}

    }

}
