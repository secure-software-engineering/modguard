package de.upb.mod2.api;


import java.util.HashSet;
import java.util.Collection;

import de.upb.myannotation.Critical;

public class Leaker {


	public Collection<byte[]> testCollection;

   	@Critical
	private byte[] secret = new byte[]{1,2,3,4,5,6};

	public String text="hi";

	public Leaker(){
		this("");
	}


	public Leaker(String text){
		this.text = text;
		this.testCollection = new HashSet<>();
	}



   public void leak(String parameter){
   		
	    this.testCollection.add(this.secret);
	}

}
