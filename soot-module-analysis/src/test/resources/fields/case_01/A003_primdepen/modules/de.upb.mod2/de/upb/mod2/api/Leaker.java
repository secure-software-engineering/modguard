package de.upb.mod2.api;
import de.upb.myannotation.Critical;

import java.io.File;

public class Leaker {

 	public int secField1 = 221<<2;

 	private int secField2 = 100;


 	@Critical
	private int secrectField = secField1 * secField2;


	public static int test(){	
		return 9;
	}


}
