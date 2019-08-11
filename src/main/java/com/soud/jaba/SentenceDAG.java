package com.soud.jaba;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SentenceDAG {
    private String sentence;
    private Tokenizer tokenizer;

    private Map<Integer, List<Integer>> dag;
    private Map<Integer, Pair<Integer, Double>> route;

    private Map<Pair<Integer, Integer>, Integer> subStringFreq = new HashMap<>();

    public SentenceDAG(String sentence, Tokenizer tokenizer) {
        this.sentence = sentence;
        this.tokenizer = tokenizer;
        this.dag = makeDAG(sentence, tokenizer);
    }

    /**
     * 采用动态规划查找最大概率路径, 找出基于词频的最大切分组合
     */
    private Map<Integer, Pair<Integer, Double>> calcMaxProbPath(String sentence, Map<Integer, List<Integer>> dag) {
        int len = sentence.length();
        Map<Integer, Pair<Integer, Double>> route = new HashMap<>();
        route.put(len, Pair.of(0, 0d));
        double logTotal = Math.log(tokenizer.getTotal());
        for (int k = len - 1; k >= 0; k--) {
            int offset = 0;
            double maxProb = -Double.MAX_VALUE;
            for (Integer x : dag.get(k)) {
                int end = k + x;
                int freq = tokenizer.getWordFreq(sentence.substring(k, end));
                subStringFreq.put(Pair.of(k, end), freq);
                double prob = Math.log(freq > 0 ? freq : 1) - logTotal + route.get(end).getValue();
                if (maxProb < prob) {
                    maxProb = prob;
                    offset = end - 1;
                }
            }
            route.put(k, Pair.of(offset, maxProb));
        }
        return route;
    }

    /**
     * 基于trie查询前缀，生成句子中汉字所有可能成词情况所构成的有向无环图
     */
    static HashMap<Integer, List<Integer>> makeDAG(String sentence, Tokenizer tokenizer) {
        int len = sentence.length();
        HashMap<Integer, List<Integer>> dag = new HashMap<>();
        tokenizer.hits(sentence)
            .forEach(hit -> dag.computeIfAbsent(hit.begin, k -> new ArrayList<>()).add(hit.end - hit.begin));
        for (int k = 0; k < len; k++) {
            if (!dag.containsKey(k)) {
                List<Integer> lis = new ArrayList<>();
                lis.add(1);
                dag.put(k, lis);
            }
        }
        return dag;
    }

    public String getSentence() {
        return sentence;
    }

    public int getWordFreq(int start, int end) {
        return subStringFreq.getOrDefault(Pair.of(start, end), 0);
    }

    public Map<Integer, Pair<Integer, Double>> getRoute() {
        if (this.route == null) {
            this.route = calcMaxProbPath(sentence, this.dag);
        }
        return route;
    }
}
