package de.upb.mod2.api;
import java.io.File;
import java.io.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class LeakerFactory {



    public static void doSome(int i, String string){
        System.out.println(i+""+string);

    }

    public void generate()
            throws IOException {

        String word = "";


        char ch = 'a';

        if (0 < "".length()) {

            System.out.println("Hi");
        }

        word = word + ch;



        Map<Integer,String> map = new HashMap<>();
        map.put(1, "A");
        map.put(2, "B");
        map.put(3, "C");
        BiConsumer<Integer,String> biConsumer = (key, value) ->
                System.out.println("Key:"+ key+" Value:"+ value);
        map.forEach(biConsumer);

    }
    public static void main(String[] args){
        new LeakerFactory().gen2();
    }


    public void gen2(){
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            MethodHandle mh = lookup.findStatic(LeakerFactory.class,"doSome", MethodType.methodType(void.class,int.class,String.class));
            mh.invokeExact(5,"hi");

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}
