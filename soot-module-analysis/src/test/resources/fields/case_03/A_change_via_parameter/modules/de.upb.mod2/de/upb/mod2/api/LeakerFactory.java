package de.upb.mod2.api;

import de.upb.mod2.internal.LeakerImpl;
import java.lang.invoke.*;
import java.lang.reflect.*;

public class LeakerFactory {


	
    public static Leaker getInstance(){
  
    	return new LeakerImpl();
    
    }


}
