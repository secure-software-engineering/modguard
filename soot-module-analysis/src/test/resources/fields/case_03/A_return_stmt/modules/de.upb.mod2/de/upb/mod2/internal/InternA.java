package de.upb.mod2.internal;

import de.upb.mod2.api.Leaker;
import de.upb.myannotation.Critical;

public class InternA  {

	@Critical
	public byte[] key; 

	public byte[] baseKey;

	public InternA(){
		//this is the secret key
		key = new byte[]{1,2,3,4,5,6};
	}

	public void computeSecretKey(){
		for(int i=0; i<key.length; i++){
			this.key[i] = (byte) (this.key[i] ^ this.baseKey[i]);
		}
	}


}