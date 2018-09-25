package de.upb.mod2.api;

import de.upb.myannotation.Critical;


@Critical
public interface Leaker {

    Object leak(String parameter);
}
