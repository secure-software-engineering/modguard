package de.upb.mod2.internal;

import de.upb.mod2.api.Leaker;

import de.upb.mod2.api.B;
import de.upb.mod2.api.A;

public class LeakerImpl implements Leaker {

    // this currently, does not work because

    public void leak(A parameter, boolean value) {

        //if(value)
        byte[] secret = new byte[] {1, 2, 3, 4, 5, 6};
        B myB = new B();
        myB.secret = secret;

        parameter.callback(myB);
        //  else
        //  parameter.callback(null);
    }
}
