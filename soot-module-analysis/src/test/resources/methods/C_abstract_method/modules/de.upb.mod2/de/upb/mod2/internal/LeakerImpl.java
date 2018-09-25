package de.upb.mod2.internal;

import de.upb.mod2.api.Leaker;



public class LeakerImpl extends Leaker{



    @Override
    public void leak(String parameter) {
    	System.out.println(parameter);
    }
}
