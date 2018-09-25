package de.upb.mod2.internal;

import de.upb.mod2.api.Leaker;


public class LeakerImpl implements Leaker{

    //this is key usable by other
    private InternA internalA = new InternA();

    @Override
    public Object leak(String parameter) {
        return internalA;
    }


}
