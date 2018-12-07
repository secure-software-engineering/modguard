package de.upb.mod2.api;
import de.upb.myannotation.Critical;

import java.io.File;

public class Leaker {


	@Critical
	private int secretKey = Leaker.partFeld1 ^ Leaker.partFeld2;



	private static byte partFeld1  = 0x05;


	//this is accessible from outside and part of the secret field
	public static byte partFeld2 = 0x06;

	public String text="hi";


	public Leaker(){
		this("");

	}


	public Leaker(String text){
		this.text = text;
	}



   public void leak(String parameter){
    	System.out.println("hi");
	}


}
