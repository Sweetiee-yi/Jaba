结巴分词(java版) jaba
===============================


感谢jieba分词原作者[fxsjy](https://github.com/fxsjy)，本项目实现了 java 版本的 jieba。

创建此项目起因：[jieba-analysis](https://github.com/huaban/jieba-analysis)这个项目分词的结果和python版本不一致，还会把英文字母全部改为小写。所以我重新实现了一下 java 版本的 jieba，保证了分词结果和 python 版本一致。


简介
====

支持分词模式
------------

-   CUT——精确模式，试图将句子最精确地切开，适合文本分析。
-   CUT_ALL——全模式，把句子中所有的可以成词的词语都扫描出来, 速度非常快，但是不能解决歧义。
-   CUT_WITHOUT_HMM——精确模式，但不使用HMM识别未登录词。

如何使用
========

-   Demo

``` {.java}

@Test
public void testDemo() {
    Jaba jaba = new Jaba();
    String[] sentences =
            new String[] {"这是一个伸手不见五指的黑夜。我叫孙悟空，我爱北京，我爱Python和C++。", "我不喜欢日本和服。", "雷猴回归人间。",
                    "工信处女干事每月经过下属科室都要亲口交代24口交换机等技术性器件的安装工作", "结果婚的和尚未结过婚的"};
    for (String sentence : sentences) {
        System.out.println(jaba.cut(sentence, CutModeEnum.CUT).toString());
    }
}
```

算法
=================

-   \[ \] 基于 `DoubleArrayTrie` 树结构存储的词典，性能比 `trie` 树更好
-   \[ \] 生成所有切词可能的有向无环图 `DAG`
-   \[ \] 采用动态规划算法计算最佳切词组合
-   \[ \] 基于 `HMM` 模型，采用 `Viterbi` (维特比)算法实现未登录词识别

