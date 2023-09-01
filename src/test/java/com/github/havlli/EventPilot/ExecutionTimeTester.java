package com.github.havlli.EventPilot;

public class ExecutionTimeTester {

    public static void printExecutionTime(String name, Runnable runnable) {
        long beforeExecutionTime = System.nanoTime();
        runnable.run();
        long afterExecutionTime = System.nanoTime();
        System.out.printf("Execution time \t%d\t\t%s%n", afterExecutionTime - beforeExecutionTime, name);
    }
}
