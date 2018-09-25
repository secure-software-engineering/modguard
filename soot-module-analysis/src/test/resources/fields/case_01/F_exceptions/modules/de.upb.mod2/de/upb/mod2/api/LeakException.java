package de.upb.mod2.api;

public class LeakException extends RuntimeException{

	public Object o;

	public LeakException(Object o){
		this.o = o;
	}


}