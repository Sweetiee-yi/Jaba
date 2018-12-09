package com.soud.jaba.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Soud
 */
public class RegexSplitUtils {

    /**
     *  根据 pattern 把字符串 str 分割出来，匹配与不匹配部分皆保留。
     *  详见：com.soud.jaba.JabaCutTest#testSplit()
     */
    public static List<String> split(Pattern pattern, String str) {
        List<String> result = new ArrayList<>();
        List<Integer> positions = new ArrayList<>();
        Matcher m = pattern.matcher(str);
        positions.add(0);
        while(m.find()) {
            positions.add(m.start());
            positions.add(m.end());
        }
        positions.add(str.length());
        for(int i = 0; i < positions.size() - 1; i++) {
            int st = positions.get(i);
            int ed = positions.get(i + 1);
            if (ed != st) {
                result.add(str.substring(st, ed));
            }
        }
        return result;
    }
}
