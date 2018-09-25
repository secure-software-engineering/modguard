package de.upb.mod2.internal;

import de.upb.mod2.api.Leaker;
import de.upb.myannotation.Critical;

public class InternA  {

	@Critical
	public byte[] key; 

	public InternA(){
		//this is the secret key
		key = new byte[]{1,2,3,4,5,6};
	}

	public byte[] getKey(){
		return key;
	}
	    

	

}