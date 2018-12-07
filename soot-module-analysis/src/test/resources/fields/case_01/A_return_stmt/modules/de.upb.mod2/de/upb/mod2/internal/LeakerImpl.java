package de.upb.mod2.internal;

import de.upb.mod2.api.Leaker;
import de.upb.myannotation.Critical;


public class LeakerImpl implements Leaker{

    @Critical
    byte[] secretKey = new byte[]{1,2,3,4,5};
	

    @Override
    public Object leak() {
      //  if(parameter == "hi")
            return this.secretKey;
     //  else
      //      return null;
      //  return internalMethod(true);
    }


    private byte[] internalMethod(boolean value){   
        
    	if(value)
    		return this.secretKey;
    	else
    		return null;
    }
}
