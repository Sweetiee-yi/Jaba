package com.soud.jaba.util;

import com.soud.jaba.SentenceDAG;

public class WordBuilder {
    private String sentence;

    private int start;
    private int end;
    private int wordFreq;
    private SentenceDAG sentenceDAG;
    private boolean empty = true;

    public WordBuilder(SentenceDAG sentenceDAG) {
        this.sentenceDAG = sentenceDAG;
        this.sentence = sentenceDAG.getSentence();
        this.start = -1;
        this.end = 0;
    }

    public int length() {
        return empty ? 0 : this.end - this.start;
    }

    public boolean isEmpty() {
        return empty;
    }

    public void append(int pos) {
        empty = false;
        if (this.start == -1) {
            this.start = pos - 1;
        }
        this.end = pos;
    }

    public String build() {
        String result = this.sentence.substring(this.start, this.end);
        this.start = -1;
        empty = true;
        return result;
    }

    public String build(boolean queryWordFreq) {
        String result = this.sentence.substring(this.start, this.end);
        if (queryWordFreq) {
            wordFreq = this.sentenceDAG.getWordFreq(this.start, this.end);
        }
        this.start = -1;
        empty = true;
        return result;
    }

    public int getWordFreq() {
        return wordFreq;
    }
}
