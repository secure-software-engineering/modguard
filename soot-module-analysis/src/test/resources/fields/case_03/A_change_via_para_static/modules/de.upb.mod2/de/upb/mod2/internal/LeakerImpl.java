package de.upb.mod2.internal;

import de.upb.mod2.api.Leaker;
import de.upb.myannotation.Critical;


public class LeakerImpl extends Leaker{

    // public key use for all operations
    @Critical
	public byte[] key = new byte[]{0,0,0,0,0,};

    @Override
    public Object doSome(byte[] parameter) {
        	return null;
    }




}
