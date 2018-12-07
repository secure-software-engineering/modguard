package de.upb.mod2.internal;

import de.upb.mod2.api.Leaker;
import de.upb.myannotation.Critical;


public class LeakerImpl implements Leaker{

    byte[] secretKey = new byte[]{1,2,3,4,5};
	

    public Object leak(String parameter) {
        return internalMethod(true);
    }

    @Critical
    private byte[] internalMethod(boolean value){
        
    	if(value)
    		return this.secretKey;
    	else
    		return null;
    }
}
