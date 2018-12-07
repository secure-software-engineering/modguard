package de.upb.mod2.api;
import de.upb.myannotation.Critical;

import java.io.File;

public class Leaker {

 	@Critical
 	public int secretField = 1;



	public static Integer test2(){

		return new Integer(2);
	}

	
}
