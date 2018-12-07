package de.upb.mod2.internal;

import de.upb.mod2.api.Leaker;

import de.upb.mod2.api.LeakException;


import de.upb.myannotation.Critical;

public class LeakerImpl implements Leaker{
    @Critical

    private byte[] secret = new byte[]{1,2,3,4,5,6};

    @Override
    public void leak(String parameter) {
        internalMethod();
    }


    private void internalMethod(){
    	System.out.println("internal Method called!");
    	
    	throw new LeakException(this.secret);
    }
}
