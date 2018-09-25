package de.upb.mod2.internal;

import de.upb.mod2.api.Leaker;

import de.upb.myannotation.Critical;

public class LeakerImpl implements Leaker{

	@Critical
	private int secret = 666;

    @Override
    public void leak(int[] parameter) {
    		
      		parameter[0] =  this.secret;
    }
}
