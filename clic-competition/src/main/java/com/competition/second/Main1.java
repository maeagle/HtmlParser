package com.competition.second;

import java.util.*;

public class Main1 {


    public static Map<Integer, List<String>> pageKeywordMap = new HashMap<>();

    public static Map<Integer, List<String>> queryKeywordMap = new HashMap<>();

    public static void main(String[] args) {
        Scanner console = new Scanner(System.in);

        int pageCount = 0;
        if (console.hasNextLine())
            pageCount = console.nextInt();

        int queryCount = 0;
        if (console.hasNextLine())
            queryCount = console.nextInt();


        while (pageCount != 0 && console.hasNextLine()) {
            String consoleText = console.nextLine();
            if (consoleText == null || consoleText.trim().equals(""))
                continue;
            List<String> vals = new ArrayList<>(Arrays.asList(consoleText.split(" ")));
            String no = vals.get(0);
            vals = vals.subList(1, vals.size());
            for (int i = 0; i < vals.size(); i++) {
                if (vals.get(i).length() > 20)
                    vals.remove(i);
            }
            if (vals.size() > 10)
                vals = vals.subList(0, 10);
            pageKeywordMap.put(Integer.parseInt(no.substring(1)), vals);
            pageCount--;
        }

        while (queryCount != 0 && console.hasNextLine()) {
            String consoleText = console.nextLine();
            if (consoleText == null || consoleText.trim().equals(""))
                continue;
            List<String> vals = new ArrayList<>(Arrays.asList(consoleText.split(" ")));
            String no = vals.get(0);
            vals = vals.subList(1, vals.size());
            for (int i = 0; i < vals.size(); i++) {
                if (vals.get(i).length() > 20)
                    vals.remove(i);
            }
            vals = new ArrayList<>(vals);
            for (int i = vals.size() - 1; i >= 0; i--) {
                for (int j = 0; j < i && j < vals.size(); j++) {
                    if (vals.get(i).equalsIgnoreCase(vals.get(j))) {
                        vals.remove(i);
                        break;
                    }
                }
            }
            if (vals.size() > 10)
                vals = vals.subList(0, 10);
            queryKeywordMap.put(Integer.parseInt(no.substring(1)), vals);
            queryCount--;
        }
        console.close();

        for (int queryNo : queryKeywordMap.keySet()) {
            Map<String, Integer> pageKeywordsRank = getPageKeywordRank(queryNo);
            System.out.println(makeQueryResultString(queryNo, pageKeywordsRank));
        }
    }


    public static String makeQueryResultString(int queryNo, Map<String, Integer> pageKeywordsRank) {
        List<String> pageOrderList = new ArrayList<>();
        while (pageOrderList.size() < pageKeywordsRank.size()) {
            Iterator<Map.Entry<String, Integer>> rankIter = pageKeywordsRank.entrySet().iterator();
            String currentMaxPage = null;
            int currentMaxRank = 0;
            while (rankIter.hasNext()) {
                Map.Entry<String, Integer> item = rankIter.next();
                if (!pageOrderList.contains(item.getKey())
                        && currentMaxRank < item.getValue()) {
                    currentMaxRank = item.getValue();
                    currentMaxPage = item.getKey();
                }
            }
            pageOrderList.add(currentMaxPage);
        }
        StringBuilder sb = new StringBuilder();
        if (pageOrderList.size() == 0)
            return "无查询结果";
        else {
            for (int i = 0; i < pageOrderList.size() && i <= 4; i++) {
                sb.append(pageOrderList.get(i));
                sb.append(" ");
            }
            return sb.substring(0, sb.length() - 1);
        }
    }

    public static Map<String, Integer> getPageKeywordRank(int queryNo) {

        List<String> queryKeywords = queryKeywordMap.get(queryNo);

        Map<String, Integer> result = new HashMap<>();
        Iterator<Map.Entry<Integer, List<String>>> pageKeywordIter = pageKeywordMap.entrySet().iterator();
        while (pageKeywordIter.hasNext()) {
            Map.Entry<Integer, List<String>> item = pageKeywordIter.next();
            List<String> pageKeywords = item.getValue();
            int pageNo = item.getKey();
            int pageTotalRank = 0;
            for (int queryKeywordIndex = 0; queryKeywordIndex < queryKeywords.size(); queryKeywordIndex++) {
                for (int pageKeywordIndex = 0; pageKeywordIndex < pageKeywords.size(); pageKeywordIndex++) {
                    if (queryKeywords.get(queryKeywordIndex)
                            .equalsIgnoreCase(pageKeywords.get(pageKeywordIndex))) {
                        int queryKeywordRank = 10 - queryKeywordIndex;
                        int pageKeywordRank = 10 - pageKeywordIndex;
                        pageTotalRank += queryKeywordRank * pageKeywordRank;
                    }
                }
            }
            if (pageTotalRank != 0)
                result.put("P" + pageNo, pageTotalRank);
        }
        return result;
    }
}
