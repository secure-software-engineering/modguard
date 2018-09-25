package de.upb.mod2.api;

import de.upb.myannotation.Critical;



public abstract class Leaker {


		// public key use for all operations
	@Critical
  protected byte[] key = new byte[]{0,0,0,0,0};

   public abstract Object leak();

  	public  Object getKey(){
    	return this.key;
    }
}
