package de.upb.mod2.internal;

import de.upb.mod2.api.MyKey;

import de.upb.myannotation.Critical;

public class ConfidentialKey extends MyKey {

	@Critical
	private static byte[] secretKey = {1,2,3,4};

	public ConfidentialKey(){
		super(ConfidentialKey.secretKey);
	}
	    
}