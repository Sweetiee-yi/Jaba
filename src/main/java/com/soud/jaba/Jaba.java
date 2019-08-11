package com.soud.jaba;

import com.soud.jaba.enumeration.CutModeEnum;
import com.soud.jaba.util.RegexSplitUtils;
import com.soud.jaba.util.WordBuilder;
import com.soud.jaba.viterbi.FinalSeg;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
     * 加载用户自定义字典
     * @param inputStream 字典
     */
    public void loadUserDict(InputStream inputStream) {
        tokenizer.loadDictionary(inputStream);
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

    /**
     * 根据 DAG 上算出的最大概率路径将句子分词
     */
    private List<String> cutByDAG(String sentence) {
        List<String> result = new ArrayList<>();
        SentenceDAG sentenceDAG = new SentenceDAG(sentence, tokenizer);
        Map<Integer, Pair<Integer, Double>> route = sentenceDAG.getRoute();
        int st = 0;
        int ed;
        StringBuilder sb = new StringBuilder();
        while (st < sentence.length()) {
            ed = route.get(st).getKey() + 1;
            if (ed - st == 1 && Character.isLetterOrDigit(sentence.charAt(st))) {
                sb.append(sentence.charAt(st));
            } else {
                if (sb.length() > 0) {
                    result.add(sb.toString());
                    sb.setLength(0);
                }
                result.add(sentence.substring(st, ed));
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
    private void doIfNeedHMM(WordBuilder wb, List<String> result) {
        if (!wb.isEmpty()) {
            if (wb.length() == 1) {
                result.add(wb.build());
            } else {
                String str = wb.build(true);
                if (wb.getWordFreq() == 0) {
                    result.addAll(finalSeg.cut(str));
                } else {
                    result.addAll(Arrays.asList(str.split("")));
                }
            }
        }
    }

    /**
     * 根据 DAG 上算出的最大概率路径将句子分词，并使用 HMM 识别可能出现的未登录词
     */
    private List<String> cutByDAGWithHMM(String sentence) {
        List<String> result = new ArrayList<>();
        SentenceDAG sentenceDAG = new SentenceDAG(sentence, tokenizer);
        Map<Integer, Pair<Integer, Double>> route = sentenceDAG.getRoute();
        int len = sentence.length();
        int st = 0;
        int ed;
        String word;
        WordBuilder wordBuilder = new WordBuilder(sentenceDAG);
        while (st < len) {
            ed = route.get(st).getKey() + 1;
            word = sentence.substring(st, ed);
            if (word.length() == 1) {
                wordBuilder.append(ed);
            } else {
                doIfNeedHMM(wordBuilder, result);
                result.add(word);
            }
            st = ed;
        }
        doIfNeedHMM(wordBuilder, result);
        return result;
    }

    /**
     * 全模式，把句子中所有的可以成词的词语都扫描出来, 速度非常快，但是不能解决歧义
     */
    private List<String> cutAll(String sentence) {
        List<String> result = new ArrayList<>();
        Map<Integer, List<Integer>> dag = SentenceDAG.makeDAG(sentence, tokenizer);
        int[] last = {-1};
        dag.forEach((k, l) -> {
            if (l.size() == 1 && k > last[0]) {
                result.add(sentence.substring(k, k + l.get(0)));
                last[0] = k + l.get(0) - 1;
            } else {
                l.forEach(j -> {
                    result.add(sentence.substring(k, k + j));
                    last[0] = k + j - 1;
                });
            }
        });
        return result;
    }
}
