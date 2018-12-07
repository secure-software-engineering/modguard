package de.upb.mod2.api;



import de.upb.mod2.internal.CritServlet;
import de.upb.mod2.internal.PubServlet;


public class LeakerFactory {


   
   public static void leak(Context parameter, boolean value) {

   		Servlet servlet = new CritServlet();


        parameter.callback(servlet);
 
    }
}
