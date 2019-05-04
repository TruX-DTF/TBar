package edu.lu.uni.serval.tbar.utils;

import java.util.List;

import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

public class JunitRunner {

    public static void main(String... args) throws ClassNotFoundException {
        String[] classAndMethod = args[0].split("#");
        Request request = Request.method(Class.forName(classAndMethod[0]), classAndMethod[1]);
        Result result = new JUnitCore().run(request);
        if (result.getFailureCount()>0){
            System.out.println("E");
        }
        System.out.println("<<");
        List<Failure> failures = result.getFailures();
        for (Failure failure: failures){
            System.out.println(failure.getMessage());
            System.out.println(failure.getDescription().toString());
            System.out.println(failure.getTrace());
        }
        System.exit(0);
    }
    
}
