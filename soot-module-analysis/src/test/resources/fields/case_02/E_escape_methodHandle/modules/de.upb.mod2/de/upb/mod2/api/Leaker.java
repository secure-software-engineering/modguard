package de.upb.mod2.api;




public abstract class Leaker {


	// public key use for all operations
	protected byte[] key = new byte[]{0,0,0,0,0,};

  	public abstract Object leak(String parameter);

 	public  Object getKey(){
    	return this.key;
    }
}
