package com.knowledge.common.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 文本差异比较工具类
 * 实现简单的行级diff算法
 */
public class DiffUtil {
    
    /**
     * 差异行类型
     */
    public enum DiffType {
        EQUAL,    // 相同
        INSERT,   // 新增
        DELETE    // 删除
    }
    
    /**
     * 差异行
     */
    public static class DiffLine {
        private DiffType type;
        private String content;
        private int lineNumber1; // 原文本行号
        private int lineNumber2; // 新文本行号
        
        public DiffLine(DiffType type, String content, int lineNumber1, int lineNumber2) {
            this.type = type;
            this.content = content;
            this.lineNumber1 = lineNumber1;
            this.lineNumber2 = lineNumber2;
        }
        
        public DiffType getType() {
            return type;
        }
        
        public String getContent() {
            return content;
        }
        
        public int getLineNumber1() {
            return lineNumber1;
        }
        
        public int getLineNumber2() {
            return lineNumber2;
        }
    }
    
    /**
     * 比较两个文本，返回差异列表
     * 使用简单的基于最长公共子序列(LCS)的算法
     */
    public static List<DiffLine> diff(String text1, String text2) {
        List<String> lines1 = splitLines(text1);
        List<String> lines2 = splitLines(text2);
        
        return diffLines(lines1, lines2);
    }
    
    /**
     * 按行分割文本
     */
    private static List<String> splitLines(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(text.split("\n", -1));
    }
    
    /**
     * 比较两个文本行列表
     */
    private static List<DiffLine> diffLines(List<String> lines1, List<String> lines2) {
        List<DiffLine> result = new ArrayList<>();
        
        int m = lines1.size();
        int n = lines2.size();
        
        // 使用动态规划计算LCS长度
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (lines1.get(i - 1).equals(lines2.get(j - 1))) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        
        // 回溯构建diff结果
        int i = m, j = n;
        int lineNum1 = m, lineNum2 = n;
        
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && lines1.get(i - 1).equals(lines2.get(j - 1))) {
                // 相同行
                result.add(0, new DiffLine(DiffType.EQUAL, lines1.get(i - 1), i, j));
                i--;
                j--;
            } else if (j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j])) {
                // 新增行（在新文本中）
                result.add(0, new DiffLine(DiffType.INSERT, lines2.get(j - 1), 0, j));
                j--;
            } else if (i > 0) {
                // 删除行（在原文本中）
                result.add(0, new DiffLine(DiffType.DELETE, lines1.get(i - 1), i, 0));
                i--;
            }
        }
        
        return result;
    }
    
    /**
     * 获取统计信息：新增行数、删除行数、相同行数
     */
    public static DiffStats getStats(List<DiffLine> diffLines) {
        int insertCount = 0;
        int deleteCount = 0;
        int equalCount = 0;
        
        for (DiffLine line : diffLines) {
            switch (line.getType()) {
                case INSERT:
                    insertCount++;
                    break;
                case DELETE:
                    deleteCount++;
                    break;
                case EQUAL:
                    equalCount++;
                    break;
            }
        }
        
        return new DiffStats(insertCount, deleteCount, equalCount);
    }
    
    /**
     * 差异统计
     */
    public static class DiffStats {
        private int insertCount;
        private int deleteCount;
        private int equalCount;
        
        public DiffStats(int insertCount, int deleteCount, int equalCount) {
            this.insertCount = insertCount;
            this.deleteCount = deleteCount;
            this.equalCount = equalCount;
        }
        
        public int getInsertCount() {
            return insertCount;
        }
        
        public int getDeleteCount() {
            return deleteCount;
        }
        
        public int getEqualCount() {
            return equalCount;
        }
    }
}

