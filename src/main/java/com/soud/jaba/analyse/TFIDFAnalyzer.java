package com.soud.jaba.analyse;

import com.soud.jaba.Jaba;
import com.soud.jaba.Pair;
import com.soud.jaba.enumeration.CutModeEnum;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

/**
 * tf-idf提取关键词
 *
 * @author Soud
 */
public class TFIDFAnalyzer {
  private HashMap<String, Double> idfMap;
  private HashSet<String> stopWords;
  private double medianIdf;

  private static class TFIDFAnalyzerHolder {
    static TFIDFAnalyzer instance = new TFIDFAnalyzer();
  }

  public static TFIDFAnalyzer getInstance() {
    return TFIDFAnalyzer.TFIDFAnalyzerHolder.instance;
  }

  private TFIDFAnalyzer() {
    this.loadStopWords(this.getClass().getResourceAsStream("/stop_words.txt"));
    this.loadIdfMap(this.getClass().getResourceAsStream("/idf.txt"));
  }

  public List<Pair<String, Double>> extractTags(List<String> words, int topK) {
    Map<String, Double> tfMap = getTfMap(words);
    List<Pair<String, Double>> result = new ArrayList<>();
    for (String word : tfMap.keySet()) {
      result.add(new Pair<>(word, idfMap.getOrDefault(word, medianIdf) * tfMap.get(word)));
    }
    result.sort((o1, o2) -> Double.compare(o2.getValue(), o1.getValue()));
    if (result.size() > topK) {
      result = result.subList(0, topK);
    }
    return result;
  }

  public List<Pair<String, Double>> extractTags(List<String> words) {
    return extractTags(words, 20);
  }

  public List<Pair<String, Double>> extractTags(String sentence, int topK) {
    return extractTags(Jaba.getInstance().cut(sentence, CutModeEnum.CUT), topK);
  }

  public List<Pair<String, Double>> extractTags(String sentence) {
    return extractTags(Jaba.getInstance().cut(sentence, CutModeEnum.CUT));
  }

  private Map<String, Double> getTfMap(List<String> tokens) {
    Map<String, Double> tfMap = new HashMap<>();

    Map<String, Long> freqMap = tokens.stream()
        .filter(token -> token.trim().length() > 1 && !stopWords.contains(token.toLowerCase()))
        .collect(groupingBy(Function.identity(), counting()));
    long tokenNum = freqMap.values().stream().mapToLong(t -> t).sum();

    for (String word : freqMap.keySet()) {
      tfMap.put(word, 1.0 * freqMap.get(word) / tokenNum);
    }

    return tfMap;
  }


  public void loadStopWords(InputStream resourceStream) {
    stopWords = new HashSet<>();
    try (BufferedReader buffer = new BufferedReader(new InputStreamReader(resourceStream, StandardCharsets.UTF_8))) {
      String line;
      while ((line = buffer.readLine()) != null) {
        stopWords.add(line);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void loadIdfMap(InputStream resourceStream) {
    idfMap = new HashMap<>();
    try (BufferedReader buffer = new BufferedReader(new InputStreamReader(resourceStream, StandardCharsets.UTF_8))) {
      String line;
      while ((line = buffer.readLine()) != null) {
        String[] kv = line.trim().split(" ");
        idfMap.put(kv[0], Double.parseDouble(kv[1]));
      }
      List<Double> idfList = new ArrayList<>(idfMap.values());
      Collections.sort(idfList);
      medianIdf = idfList.get(idfList.size() / 2);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
