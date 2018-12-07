package de.upb.mod2.internal;

import de.upb.mod2.api.Leaker;
import de.upb.myannotation.Critical;


public class LeakerImpl extends Leaker{

    // public key use for all operations
    @Critical
	private byte[] key = new byte[]{0,0,0,0,0,};

    @Override
    public Object modify(byte[] parameter) {
        	this.key = parameter;
        	return null;
    }




}
