package com.knowledge.search.util;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

/**
 * 拼音工具类
 */
public class PinyinUtil {

    private static final HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();

    static {
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
    }

    /**
     * 获取字符串的拼音（全拼）
     * @param text 输入文本
     * @return 拼音字符串，多个字用空格分隔
     */
    public static String getPinyin(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        StringBuilder pinyin = new StringBuilder();
        char[] chars = text.toCharArray();

        for (char c : chars) {
            if (Character.toString(c).matches("[\\u4E00-\\u9FA5]+")) {
                // 中文字符
                try {
                    String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c, format);
                    if (pinyinArray != null && pinyinArray.length > 0) {
                        pinyin.append(pinyinArray[0]).append(" ");
                    }
                } catch (BadHanyuPinyinOutputFormatCombination e) {
                    // 转换失败，保留原字符
                    pinyin.append(c).append(" ");
                }
            } else if (Character.isLetterOrDigit(c)) {
                // 英文字母或数字，转换为小写
                pinyin.append(Character.toLowerCase(c)).append(" ");
            } else {
                // 其他字符，保留原样
                pinyin.append(c).append(" ");
            }
        }

        return pinyin.toString().trim();
    }

    /**
     * 获取字符串的首字母
     * @param text 输入文本
     * @return 首字母字符串
     */
    public static String getInitial(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        StringBuilder initial = new StringBuilder();
        char[] chars = text.toCharArray();

        for (char c : chars) {
            if (Character.toString(c).matches("[\\u4E00-\\u9FA5]+")) {
                // 中文字符
                try {
                    String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c, format);
                    if (pinyinArray != null && pinyinArray.length > 0) {
                        initial.append(pinyinArray[0].charAt(0));
                    }
                } catch (BadHanyuPinyinOutputFormatCombination e) {
                    // 转换失败，跳过
                }
            } else if (Character.isLetter(c)) {
                // 英文字母，取首字母
                initial.append(Character.toLowerCase(c));
            } else if (Character.isDigit(c)) {
                // 数字，保留
                initial.append(c);
            }
            // 其他字符跳过
        }

        return initial.toString();
    }
}

