package com.soud.jaba.viterbi;

import com.soud.jaba.util.RegexSplitUtils;
import com.sun.tools.javac.util.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 使用 viterbi 算法识别出未登录词
 *
 * @author Soud
 */
public class FinalSeg {
    private static final Pattern RE_HAN = Pattern.compile("([\u4E00-\u9FD5]+)");
    private static final Pattern RE_SKIP = Pattern.compile("([a-zA-Z0-9]+(?:\\.\\d+)?%?)");
    private static final double MIN_LOG_PROB_VALUE = -3.14e+100;
    /**
     * 四种状态 B:begin, M:middle, E:end, S:single
     */
    private static char[] states = new char[]{'B', 'M', 'E', 'S'};
    private static Map<Character, char[]> prevStatus;
    private static final String EMIT_P_PATH = "/prob_emit.txt";
    private HashSet<String> forceSplitWords = new HashSet<>();
    /**
     * 状态之间的转移概率
     */
    private HashMap<Pair<Character, Character>, Double> transP = new HashMap<>();
    /**
     * 状态的开始概率
     */
    private HashMap<Character, Double> startP = new HashMap<>();
    /**
     * 单字与每种状态的发射概率
     */
    private HashMap<Character, HashMap<Character, Double>> emitP = new HashMap<>();

    private FinalSeg() {
        loadModel();
    }

    private static class FinalSegHolder {
        static FinalSeg instance = new FinalSeg();
    }

    public static FinalSeg getInstance() {
        return FinalSeg.FinalSegHolder.instance;
    }

    private void loadModel() {
        prevStatus = new HashMap<>();
        prevStatus.put('B', new char[]{'E', 'S'});
        prevStatus.put('M', new char[]{'M', 'B'});
        prevStatus.put('S', new char[]{'S', 'E'});
        prevStatus.put('E', new char[]{'B', 'M'});

        // load from:
        // https://github.com/fxsjy/jieba/blob/master/jieba/finalseg/prob_start.py
        // https://github.com/fxsjy/jieba/blob/master/jieba/finalseg/prob_trans.py
        // https://github.com/fxsjy/jieba/blob/master/jieba/finalseg/prob_emit.py
        startP.put('B', -0.26268660809250016);
        startP.put('E', -3.14e+100);
        startP.put('M', -3.14e+100);
        startP.put('S', -1.4652633398537678);

        transP.put(new Pair<>('B', 'E'), -0.510825623765990);
        transP.put(new Pair<>('B', 'M'), -0.916290731874155);
        transP.put(new Pair<>('E', 'B'), -0.5897149736854513);
        transP.put(new Pair<>('E', 'S'), -0.8085250474669937);
        transP.put(new Pair<>('M', 'E'), -0.33344856811948514);
        transP.put(new Pair<>('M', 'M'), -1.2603623820268226);
        transP.put(new Pair<>('S', 'B'), -0.7211965654669841);
        transP.put(new Pair<>('S', 'S'), -0.6658631448798212);

        InputStream inputStream = getClass().getResourceAsStream(EMIT_P_PATH);
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
            HashMap<Character, Double> emitProbMap = null;
            while (bufferedReader.ready()) {
                String line = bufferedReader.readLine();
                String[] tokens = line.split("\t");
                if (tokens.length == 1) {
                    emitProbMap = new HashMap<>();
                    emitP.put(tokens[0].charAt(0), emitProbMap);
                } else {
                    assert emitProbMap != null;
                    emitProbMap.put(tokens[0].charAt(0), Double.valueOf(tokens[1]));
                }
            }
        } catch (IOException e) {
            System.err.println(String.format(Locale.getDefault(), "%s load failed!", EMIT_P_PATH));
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                System.err.println(String.format(Locale.getDefault(), "%s close failed!", EMIT_P_PATH));
            }
        }

    }

    public void addForceSplitWord(String word) {
        forceSplitWords.add(word);
    }

    private List<String> viterbi(String sentence) {
        List<String> tokens = new ArrayList<>();
        List<Map<Character, Double>> v = new ArrayList<>();
        Map<Character, Node> path = new HashMap<>();

        // 初始化路径上的概率
        v.add(new HashMap<>(4));
        for (char state : states) {
            Double emP = emitP.get(state).getOrDefault(sentence.charAt(0), MIN_LOG_PROB_VALUE);
            v.get(0).put(state, startP.get(state) + emP);
            path.put(state, new Node(state, null));
        }

        //对于句子
        for (int i = 1; i < sentence.length(); i++) {
            v.add(new HashMap<>(4));
            Map<Character, Node> newPath = new HashMap<>();
            for (char y : states) {
                Double prob = emitP.get(y).getOrDefault(sentence.charAt(i), MIN_LOG_PROB_VALUE);
                Pair<Character, Double> candidate = null;
                for (char y0 : prevStatus.get(y)) {
                    Double p = transP.getOrDefault(new Pair<>(y0, y), MIN_LOG_PROB_VALUE);
                    p += (prob + v.get(i - 1).get(y0));
                    if (candidate == null || candidate.snd <= p) {
                        candidate = new Pair<>(y0, p);
                    }
                }
                assert candidate != null;
                v.get(v.size() - 1).put(y, candidate.snd);
                newPath.put(y, new Node(y, path.get(candidate.fst)));
            }
            path = newPath;
        }

        // 回溯找到viterbi计算出的最大概率的路径
        Map<Character, Double> lastElement = v.get(v.size() - 1);
        double probE = lastElement.get('E');
        double probS = lastElement.get('S');
        LinkedList<Character> posList = new LinkedList<>();
        Node node = path.get('E');
        if (probE < probS) {
            node = path.get('S');
        }
        while (node != null) {
            posList.addFirst(node.getValue());
            node = node.getParent();
        }

        // 根据状态序列分词
        int begin = 0, next = 0;
        for (int i = 0; i < sentence.length(); ++i) {
            char pos = posList.get(i);
            if (pos == 'B') {
                begin = i;
            } else if (pos == 'E') {
                tokens.add(sentence.substring(begin, i + 1));
                next = i + 1;
            } else if (pos == 'S') {
                tokens.add(sentence.substring(i, i + 1));
                next = i + 1;
            }
        }
        if (next < sentence.length()) {
            tokens.add(sentence.substring(next));
        }

        return tokens;
    }

    public List<String> cut(String sentence) {
        List<String> result = new ArrayList<>();
        // 将句子分割成多个中文短句和非中文短句
        for (String block : RegexSplitUtils.split(RE_HAN, sentence)) {
            if (RE_HAN.matcher(block).matches()) {
                // 对于中文短句，使用viterbi划分未登录词
                for (String word : viterbi(block)) {
                    if (forceSplitWords.contains(word)) {
                        result.addAll(Arrays.asList(word.split("")));
                    } else {
                        result.add(word);
                    }
                }
            } else {
                // 处理非中文短句
                result.addAll(RegexSplitUtils.split(RE_SKIP, block));
            }
        }
        return result;
    }
}
