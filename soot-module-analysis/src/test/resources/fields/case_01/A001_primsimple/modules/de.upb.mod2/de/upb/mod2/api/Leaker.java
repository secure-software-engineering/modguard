package de.upb.mod2.api;
import de.upb.myannotation.Critical;

import java.io.File;

public class Leaker {

 	@Critical
 	public int[] secretField = new int[]{1,2};



	public static Integer test2(){

		return new Integer(2);
	}

	
}
