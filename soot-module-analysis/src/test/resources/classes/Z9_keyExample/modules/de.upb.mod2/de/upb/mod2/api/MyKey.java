package de.upb.mod2.api;


public abstract class MyKey {

  private byte[] key = new byte[]{0,0,0,0,0};

   public MyKey(byte[] key){
   	this.key = key;
   }

  	public  Object getKey(){
    	return this.key;
    }
}
