package de.upb.mod2.internal;

import de.upb.mod2.api.Leaker;
import de.upb.myannotation.Critical;

public class InternA extends LeakerImpl {


	//this is the secret key
	@Critical
	public byte[] key = new byte[]{1,2,3,4,5,6};

	    

	@Override
	public Object leak(String parameter){
	    	return key;
	 }


}