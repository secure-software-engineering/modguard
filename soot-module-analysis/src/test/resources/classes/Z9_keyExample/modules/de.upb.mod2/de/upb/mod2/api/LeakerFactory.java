package de.upb.mod2.api;

import de.upb.mod2.internal.ConfidentialKey;

public class LeakerFactory {

    public static MyKey getInstance(){
    	return	new ConfidentialKey();        
    }


}
