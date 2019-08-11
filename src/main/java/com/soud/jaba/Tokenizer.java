package com.soud.jaba;

import com.hankcs.algorithm.AhoCorasickDoubleArrayTrie;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

/**
 * 加载字典，并提供：
 * 1. 查询词语的词频
 * 2. 根据trie查询得到某字符串在字典的所有前缀匹配
 * python 版本的 jieba 是把每个词的所有前缀都放入到一个 dict 中去的，
 * 在 java 下可以使用 AhoCorasickDoubleArrayTrie 来完成同样的功能，
 *
 * @author Soud
 */
class Tokenizer {

    private static final String DICT_PATH = "/dict.txt";
    private int total = 0;
    private List<String> words = new ArrayList<>();
    private List<Integer> wordFreqs = new ArrayList<>();
    private AhoCorasickDoubleArrayTrie<Integer> acTrie;

    private Tokenizer() {
        InputStream inputStream = getClass().getResourceAsStream(DICT_PATH);
        this.loadDictionary(inputStream);
    }

    private static class TokenizerHolder {
        static Tokenizer instance = new Tokenizer();
    }

    static Tokenizer getInstance() {
        return TokenizerHolder.instance;
    }

    /**
     * 加载字典
     */
    void loadDictionary(InputStream inputStream) {
        TreeMap<String, Integer> freqMap = new TreeMap<>();
        TreeMap<String, Integer> acTrieMap = new TreeMap<>();
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            while (bufferedReader.ready()) {
                String line = bufferedReader.readLine();
                String[] tokens = line.split(" ");
                String word = tokens[0];
                Integer freq = Integer.valueOf(tokens[1]);
                freqMap.put(word, freq);
                total += freq;
            }
            // 由于trie需要传入的字典要求保证字典序，所以使用TreeMap先预缓存字典。
            freqMap.forEach((w, f) -> {
                acTrieMap.put(w, words.size());
                words.add(w);
                wordFreqs.add(f);
            });
            acTrie = new AhoCorasickDoubleArrayTrie<>();
            acTrie.build(acTrieMap);
        } catch (IOException e) {
            System.err.println(String.format(Locale.getDefault(), "%s load failed!", DICT_PATH));
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                System.err.println(String.format(Locale.getDefault(), "%s close failed!", DICT_PATH));
            }
        }
    }

    /**
     * 查询词语的词频
     */
    int getWordFreq(String word) {
        int idx = acTrie.exactMatchSearch(word);
        return idx < 0 ? 0: wordFreqs.get(idx);
    }

    List<AhoCorasickDoubleArrayTrie.Hit<Integer>> hits(String word) {
        return acTrie.parseText(word);
    }

    int getTotal() {
        return total;
    }
}
