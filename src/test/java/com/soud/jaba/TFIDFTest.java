package com.soud.jaba;

import com.soud.jaba.analyse.TFIDFAnalyzer;
import org.junit.Test;


public class TFIDFTest {
  @Test
  public void test1() {
    String sentence = "孩子上了幼儿园 安全防拐教育要做好";
    TFIDFAnalyzer.getInstance().extractTags(sentence).forEach(System.out::println);
  }

  @Test
  public void test2() {
    String sentence = "此外，公司拟对全资子公司吉林欧亚置业有限公司增资4.3亿元，增资后，吉林欧亚置业注册资本由7000万元增加到5亿元。吉林欧亚置业主要经营范围为房地产开发及百货零售等业务。目前在建吉林欧亚城市商业综合体项目。2013年，实现营业收入0万元，实现净利润-139.13万元。";
    TFIDFAnalyzer.getInstance().extractTags(sentence, 20).forEach(System.out::println);
  }
}
