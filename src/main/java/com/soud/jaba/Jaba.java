package com.soud.jaba;

import com.soud.jaba.enumeration.CutModeEnum;
import com.soud.jaba.util.RegexSplitUtils;
import com.soud.jaba.viterbi.FinalSeg;
import com.sun.tools.javac.util.Pair;

import java.util.*;
import java.util.regex.Pattern;

/**
 * jieba 的 java 版本。
 * 分词结果和 jieba 保持一致，
 * 根据小说《围城》跑50次测速，速度比 java-analysis 的实现快30%。
 *
 * @author Soud
 */
public class Jaba {

    private static final Pattern RE_HAN_DEFAULT = Pattern.compile("([\u4E00-\\u9FD5a-zA-Z0-9+#&\\._%\\-]+)");
    private static final Pattern RE_SKIP_DEFAULT = Pattern.compile("(\\r\\n|\\s)");
    private static final Pattern RE_ENG = Pattern.compile("[a-zA-Z0-9]");

    private static Tokenizer tokenizer = Tokenizer.getInstance();
    private static FinalSeg finalSeg = FinalSeg.getInstance();

    /**
     * 基于trie查询前缀，生成句子中汉字所有可能成词情况所构成的有向无环图
     */
    private HashMap<Integer, List<Integer>> makeDAG(String sentence) {
        HashMap<Integer, List<Integer>> dag = new HashMap<>();
        int len = sentence.length();
        for (int k = 0; k < len; k++) {
            List<Integer> list = new ArrayList<>();
            HashSet<String> res = tokenizer.searchPrefix(sentence.substring(k));
            for (int i = k; i < len; i++) {
                String s = sentence.substring(k, i + 1);
                if (res.contains(s)) {
                    list.add(i);
                }
            }
            if (list.isEmpty()) {
                list.add(k);
            }
            dag.put(k, list);
        }
        return dag;
    }

    /**
     * 采用动态规划查找最大概率路径, 找出基于词频的最大切分组合
     */
    private HashMap<Integer, Pair<Integer, Double>> calcMaxProbPath(String sentence, HashMap<Integer, List<Integer>> dag) {
        int len = sentence.length();
        HashMap<Integer, Pair<Integer, Double>> route = new HashMap<>();
        route.put(len, new Pair<>(0, 0d));
        double logTotal = Math.log(tokenizer.getTotal());
        for (int i = len - 1; i >= 0; i--) {
            int offset = 0;
            double maxProb = -Double.MAX_VALUE;
            for (Integer x : dag.get(i)) {
                Integer freq = tokenizer.getWordFreq(sentence.substring(i, x + 1));
                double prob = Math.log(freq > 0 ? freq : 1) - logTotal + route.get(x + 1).snd;
                if (maxProb < prob) {
                    maxProb = prob;
                    offset = x;
                }
            }
            route.put(i, new Pair<>(offset, maxProb));
        }
        return route;
    }

    /**
     * 根据 DAG 上算出的最大概率路径将句子分词
     */
    private List<String> cutByDAG(String sentence) {
        List<String> result = new ArrayList<>();
        HashMap<Integer, List<Integer>> dag = makeDAG(sentence);
        HashMap<Integer, Pair<Integer, Double>> route = calcMaxProbPath(sentence, dag);
        int len = sentence.length();
        int st = 0;
        int ed;
        String word;
        StringBuilder sb = new StringBuilder();
        while (st < len) {
            ed = route.get(st).fst + 1;
            word = sentence.substring(st, ed);
            if (word.length() == 1 && RE_ENG.matcher(word).find()) {
                sb.append(word);
            } else {
                if (sb.length() > 0) {
                    result.add(sb.toString());
                    sb.setLength(0);
                }
                result.add(word);
            }
            st = ed;
        }
        if (sb.length() > 0) {
            result.add(sb.toString());
        }
        return result;
    }

    /**
     * 如果词语不在字典内（freq==0），使用 HMM 判断是否可能是未登录词
     */
    private void doIfNeedHMM(StringBuilder sb, List<String> result) {
        if (sb.length() > 0) {
            if (sb.length() == 1) {
                result.add(sb.toString());
            } else {
                Integer freq = tokenizer.getWordFreq(sb.toString());
                if (freq == 0) {
                    result.addAll(finalSeg.cut(sb.toString()));
                } else {
                    result.addAll(Arrays.asList(sb.toString().split("")));
                }
            }
            sb.setLength(0);
        }
    }

    /**
     * 根据 DAG 上算出的最大概率路径将句子分词，并使用 HMM 识别可能出现的未登录词
     */
    private List<String> cutByDAGWithHMM(String sentence) {
        List<String> result = new ArrayList<>();
        HashMap<Integer, List<Integer>> dag = makeDAG(sentence);
        HashMap<Integer, Pair<Integer, Double>> route = calcMaxProbPath(sentence, dag);
        int len = sentence.length();
        int st = 0;
        int ed;
        String word;
        StringBuilder sb = new StringBuilder();
        while (st < len) {
            ed = route.get(st).fst + 1;
            word = sentence.substring(st, ed);
            if (word.length() == 1) {
                sb.append(word);
            } else {
                doIfNeedHMM(sb, result);
                result.add(word);
            }
            st = ed;
        }
        doIfNeedHMM(sb, result);
        return result;
    }

    /**
     * 全模式，把句子中所有的可以成词的词语都扫描出来, 速度非常快，但是不能解决歧义
     */
    private List<String> cutAll(String sentence) {
        List<String> result = new ArrayList<>();
        HashMap<Integer, List<Integer>> dag = makeDAG(sentence);
        int last[] = {-1};
        dag.forEach((k, l) -> {
            if (l.size() == 1 && k > last[0]) {
                result.add(sentence.substring(k, l.get(0) + 1));
                last[0] = l.get(0);
            } else {
                l.forEach(j -> {
                    if (j > k) {
                        result.add(sentence.substring(k, j + 1));
                        last[0] = j;
                    }
                });
            }
        });
        return result;
    }

    /**
     * 将段落/句子分词
     * @param paragraph 待分词段落
     * @param cutMode 分词模式
     * @return 分词后的结果
     */
    public List<String> cut(String paragraph, CutModeEnum cutMode) {
        List<String> result = new ArrayList<>();
        // 将段落分割成多个短句和标点符号（含换行符）
        for (String block : RegexSplitUtils.split(RE_HAN_DEFAULT, paragraph)) {
            if (RE_HAN_DEFAULT.matcher(block).matches()) {
                // 短句
                if (cutMode.isCutAll()) {
                    result.addAll(cutAll(block));
                } else if (cutMode.isHMM()) {
                    result.addAll(cutByDAGWithHMM(block));
                } else {
                    result.addAll(cutByDAG(block));
                }
            } else {
                // 标点符号或换行符
                for (String x : RegexSplitUtils.split(RE_SKIP_DEFAULT, block)) {
                    if (RE_SKIP_DEFAULT.matcher(x).find()) {
                        result.add(x);
                    } else if (!cutMode.isCutAll()) {
                        result.addAll(Arrays.asList(x.split("")));
                    } else {
                        result.add(x);
                    }
                }
            }
        }
        return result;
    }
}
