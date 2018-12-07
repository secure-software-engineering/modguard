package de.upb.mod2.internal;

import de.upb.mod2.api.Leaker;


public class LeakerImpl implements Leaker{



    @Override
    public void leak(String parameter) {
    		System.out.println(parameter);
    }
}
