package de.upb.mod2.internal;

import de.upb.mod2.api.Leaker;


public class LeakerImpl extends Leaker{

  

 

    @Override
    public Object leak(String parameter) {
        return new InternA();
    }




}
