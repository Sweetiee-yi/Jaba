package com.soud.jaba.enumeration;

/**
 * 分词模式
 * @author Soud
 */
public enum CutModeEnum {
    /**
     * 精确模式，试图将句子最精确地切开，适合文本分析。
     */
    CUT(false, true),

    /**
     * 全模式，把句子中所有的可以成词的词语都扫描出来, 速度非常快，但是不能解决歧义。
     */
    CUT_ALL(true, false),

    /**
     * 精确模式，但不使用HMM识别未登录词。
     */
    CUT_WITHOUT_HMM(false, false);


    private boolean cutAll;
    private boolean cutWithHMM;

    CutModeEnum(boolean cutAll, boolean cutWithHMM) {
        this.cutAll = cutAll;
        this.cutWithHMM = cutWithHMM;
    }

    public boolean isCutAll() {
        return cutAll;
    }

    public boolean isHMM() {
        return cutWithHMM;
    }

}
