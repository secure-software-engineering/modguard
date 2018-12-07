package de.upb.mod2.api;

import de.upb.mod2.internal.LeakerImpl;
import de.upb.mod2.internal.InternA;



public class LeakerFactory {

	public static String i = System.getProperty("hi");

    public static Leaker getInstance(){

    	
    		return new InternA();
    }


    public static byte[] getKey(Leaker leaker){

    		if(leaker instanceof LeakerImpl)
    			return ((LeakerImpl) leaker).key;
      
            
    		return  new byte[] {9,9,9,9,9,9,9};


    }

}
