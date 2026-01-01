package com.knowledge.search.util;

/**
 * 搜索类型检测工具
 * 自动识别用户输入是全文、拼音还是首字母
 */
public class SearchTypeDetector {

    /**
     * 检测搜索类型
     * @param keyword 搜索关键词
     * @return 搜索类型：FULL_TEXT, PINYIN, INITIAL
     */
    public static String detectSearchType(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return "FULL_TEXT";
        }

        keyword = keyword.trim().toLowerCase();

        // 如果只包含字母（可能是拼音或首字母）
        if (keyword.matches("^[a-z]+$")) {
            // 如果长度较短（<=3）且都是小写字母，可能是首字母
            if (keyword.length() <= 3) {
                return "INITIAL";
            }
            // 如果包含常见拼音组合，可能是拼音
            if (containsPinyinPattern(keyword)) {
                return "PINYIN";
            }
            // 否则可能是英文全文搜索
            return "FULL_TEXT";
        }

        // 包含中文或数字，使用全文搜索
        return "FULL_TEXT";
    }

    /**
     * 检测是否包含拼音模式
     */
    private static boolean containsPinyinPattern(String text) {
        // 常见的拼音组合模式
        String[] pinyinPatterns = {
            "zh", "ch", "sh", "ang", "eng", "ing", "ong",
            "ian", "iao", "uan", "uai", "uei", "uen"
        };
        
        for (String pattern : pinyinPatterns) {
            if (text.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 判断是否为纯首字母（短且都是字母）
     */
    public static boolean isLikelyInitial(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return false;
        }
        keyword = keyword.trim().toLowerCase();
        return keyword.length() <= 4 && keyword.matches("^[a-z]+$") && !containsPinyinPattern(keyword);
    }
}

