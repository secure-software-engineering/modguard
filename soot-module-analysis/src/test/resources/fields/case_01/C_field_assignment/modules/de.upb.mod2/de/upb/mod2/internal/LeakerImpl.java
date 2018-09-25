package de.upb.mod2.internal;

import de.upb.mod2.api.Leaker;

import de.upb.mod2.api.A;

import de.upb.myannotation.Critical;

public class LeakerImpl implements Leaker{



    @Override
    public void leak(A parameter) {
    		@Critical
    		byte[] secret = new byte[]{1,2,3,4,5,6};
      		parameter.field = secret;
    }
}
