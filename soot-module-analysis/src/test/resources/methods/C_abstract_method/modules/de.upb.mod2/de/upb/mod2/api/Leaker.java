package de.upb.mod2.api;

import de.upb.myannotation.Critical;



public abstract class Leaker {

	@Critical
    public abstract void leak(String parameter);
}
