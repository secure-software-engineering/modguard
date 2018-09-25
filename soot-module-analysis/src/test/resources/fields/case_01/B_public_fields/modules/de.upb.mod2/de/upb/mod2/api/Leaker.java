package de.upb.mod2.api;

import de.upb.myannotation.Critical;


public class Leaker {


	@Critical
	public byte[] secretKey;


	public String text="hi";

	public Leaker(){
		this("");

	}


	public Leaker(String text){
		this.text = text;
	}



   public void leak(String parameter){

    	this.secretKey = new byte[] {1,2,3,4,5,6};
	}

}
