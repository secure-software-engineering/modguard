package internal;

import javax.crypto.spec.SecretKeySpec;
import de.upb.myannotation.Critical;
import java.net.*;

public class MySecretKeySpec  {


    public static DatagramSocket generateFromFile() throws Exception {
        return new FileStore();
    }


    private static class FileStore extends DatagramSocket {
        @Critical
        private static InetAddress keys;

        static{
            try{
            keys = InetAddress.getByName("127.0.0.1");
        }
        catch(Exception e){
            throw new RuntimeException("Hi");
        }

        }

		public FileStore() throws Exception{
			super(80, FileStore.keys);
		}

    }

}
