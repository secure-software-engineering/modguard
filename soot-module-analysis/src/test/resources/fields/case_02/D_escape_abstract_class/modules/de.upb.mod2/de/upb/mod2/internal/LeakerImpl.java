package de.upb.mod2.internal;

import de.upb.mod2.api.Leaker;


public class LeakerImpl extends Leaker{


    private InternA internalA = new InternA();



    @Override
    public Object leak() {
        return internalA;
    }




}
