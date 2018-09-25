package de.upb.mod2.api;

import de.upb.myannotation.Critical;



public interface Leaker {

	@Critical
    void leak(String parameter);
}
