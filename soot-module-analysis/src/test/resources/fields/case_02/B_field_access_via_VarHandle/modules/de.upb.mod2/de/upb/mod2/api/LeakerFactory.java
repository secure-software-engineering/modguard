package de.upb.mod2.api;

import de.upb.mod2.internal.LeakerImpl;
import de.upb.mod2.internal.InternA;
import java.lang.invoke.*;
import java.lang.reflect.*;

public class LeakerFactory {

	private static MethodHandles.Lookup lookup = MethodHandles.lookup();

    public static Leaker getInstance(){

    
    		return new LeakerImpl();
    
    }


    public static VarHandle getFieldValue(Object obj, String fieldName) throws Exception{
        Field f = obj.getClass().getDeclaredField(fieldName);
        VarHandle handle = lookup.unreflectVarHandle(f);
        return handle;

    }

}
