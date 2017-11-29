package com.competition.second;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Main {


    public static List<List<Long>> workNoSet = new ArrayList<>();


    public static void main(String[] args) {
        Scanner console = new Scanner(System.in);

        int groupCount = 0;
        if (console.hasNextLine())
            groupCount = console.nextInt();

        while (groupCount != 0 && console.hasNextLine()) {
            String consoleText = console.nextLine();
            if (consoleText == null || consoleText.trim().equals(""))
                continue;
            List<String> vals = new ArrayList<>(Arrays.asList(consoleText.split(" ")));
            if (vals.size() > 30 || vals.size() < 2) {
                System.out.println("FALSE");
                return;
            }
            List<Long> valNums = new ArrayList<>();
            for (String val : vals) {
                long valNum = Long.parseLong(val);
                if (valNum < 3800 && valNum > 4000) {
                    System.out.println("FALSE");
                    return;
                }
                valNums.add(valNum);
            }
            workNoSet.add(valNums);
            groupCount--;
        }

        for (List<Long> workNoList : workNoSet) {
            boolean success = false;
            for (int i = 0; i < workNoList.size(); i++) {
                List<Long> compare1List = new ArrayList<>();
                List<Long> compare2List = new ArrayList<>(workNoList);
                while (compare2List.size() > 0) {
                    compare1List.add(compare2List.remove(i));
                    long result = compare(compare1List, compare2List);
                    if (result == 0) {
                        System.out.println("TRUE");
                        success = true;
                        break;
                    } else if (result > 0)
                        break;
                }
                if (success)
                    break;
            }
            if (!success)
                System.out.println("FALSE");
        }
    }

    public static long compare(List<Long> compare1List, List<Long> compare2List) {
        long list1 = 0;
        for (Long val : compare1List)
            list1 += val;
        long list2 = 0;
        for (Long val : compare2List)
            list2 += val;
        return list1 - list2;
    }

}
