package de.upb.mod2.internal;

import de.upb.mod2.api.Leaker;


import java.util.Collection;
import de.upb.myannotation.Critical;

public class LeakerImpl implements Leaker{

		@Critical
    	byte[] secret = new byte[]{1,2,3,4,5,6};

    @Override
    public void leak(Collection<Object> parameter) {
    	
      	parameter.add(secret);
    	
    }
}
