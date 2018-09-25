package de.upb.mod2.internal;

import de.upb.mod2.api.Leaker;

import de.upb.mod2.api.A;
import de.upb.myannotation.Critical;

public class LeakerImpl implements Leaker {

    @Critical
    private Double secret = 0.255;


    @Override
    public void leak(A parameter, boolean value) {

        parameter.callback(this.secret);
 
    }
}
