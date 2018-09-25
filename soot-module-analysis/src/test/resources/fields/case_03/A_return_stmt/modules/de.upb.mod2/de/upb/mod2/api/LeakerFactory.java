package de.upb.mod2.api;

import de.upb.mod2.internal.LeakerImpl;


public class LeakerFactory {

	
    public static Leaker getInstance(){
  
    	return new LeakerImpl();
    
    }


}
