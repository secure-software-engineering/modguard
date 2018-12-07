package de.upb.mod2.internal;


import de.upb.mod2.api.Servlet;
import de.upb.myannotation.Critical;

@Critical
public class CritServlet implements Servlet{


	public void doSomething(){

		System.out.println("Supposed to stay internal");

	}
}