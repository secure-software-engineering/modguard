package de.upb.mod2.internal;

import de.upb.mod2.api.Leaker;

import de.upb.mod2.api.A;
import de.upb.myannotation.Critical;



import java.util.Collection;
import java.util.HashSet;

public class LeakerImpl implements Leaker{

	@Critical
	byte[] secret = new byte[]{1,2,3,4,5,6};

    @Override
    public void leak(A parameter) {
    		parameter.fieldCol = new HashSet<byte[]>();
    		
      		parameter.fieldCol.add(secret);
    }
}
