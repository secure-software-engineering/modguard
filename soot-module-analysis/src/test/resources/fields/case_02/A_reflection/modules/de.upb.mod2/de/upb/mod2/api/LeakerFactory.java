package de.upb.mod2.api;

import de.upb.mod2.internal.LeakerImpl;
import de.upb.mod2.internal.InternA;

import java.lang.reflect.*;

public class LeakerFactory {

	public static int i = 1;

    public static Leaker getInstance(){

    	
    		return new LeakerImpl();
    
    }


// test which caller clas is used?? 
    // does this actually work? I gues no!!
    public static Object getFieldValue(Object o, String fieldName) throws Exception{

    		Field f = o.getClass().getDeclaredField(fieldName);
    		f.setAccessible(true);
    		Object ob = f.get(o);
	   		return ob;
    }

}
