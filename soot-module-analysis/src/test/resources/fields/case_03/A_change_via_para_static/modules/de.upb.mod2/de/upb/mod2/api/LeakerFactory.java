package de.upb.mod2.api;

import de.upb.mod2.internal.LeakerImpl;

public class LeakerFactory {

	
    public static Leaker getInstance(){
  
    	return new LeakerImpl();
    
    }

    public static void changeVal(Leaker a, byte[] newValue){
    	if(a instanceof LeakerImpl){
    		    	((LeakerImpl) a).key = newValue;
    	}
    	return;

    }


}
