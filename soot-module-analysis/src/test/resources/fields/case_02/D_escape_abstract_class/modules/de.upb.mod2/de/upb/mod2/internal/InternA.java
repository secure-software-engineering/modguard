package de.upb.mod2.internal;

import de.upb.mod2.api.Leaker;

import de.upb.myannotation.Critical;

public class InternA extends LeakerImpl {



	protected InternA(){
		//this is the secret key
		key = new byte[]{1,2,3,4,5,6};
	}
	    

	@Override
	public Object leak(){
		return null;
	}


}