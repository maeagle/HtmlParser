package com.competition.second;

import java.util.Scanner;

public class Question1 {

    public static void main(String[] args) {
        Scanner console = new Scanner(System.in);
        String value = "";
        while (console.hasNext()) {
            value = console.next();
            System.out.println("Echo: " + value);
            if (value.equalsIgnoreCase("EOF"))
                return;
        }
    }
}
