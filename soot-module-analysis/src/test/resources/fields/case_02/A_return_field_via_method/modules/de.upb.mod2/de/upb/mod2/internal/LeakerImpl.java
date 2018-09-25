package de.upb.mod2.internal;

import de.upb.mod2.api.Leaker;


public class LeakerImpl implements Leaker{

    //this is key usable by other
    public byte[] key = {0,0,0,0,0,0};

    @Override
    public Object leak(String parameter) {
        return "hi too";
    }


}
