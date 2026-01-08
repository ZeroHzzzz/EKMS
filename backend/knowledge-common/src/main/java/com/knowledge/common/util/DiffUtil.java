package com.knowledge.common.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 文本差异比较工具类
 * 实现行级 Diff 和 3-Way Merge 算法
 */
public class DiffUtil {
    
    public enum DiffType {
        EQUAL,    // 相同
        INSERT,   // 新增
        DELETE    // 删除
    }
    
    public static class DiffLine {
        private DiffType type;
        private String content;
        private int originalLineNumber; // 在原文本中的行号（对于INSERT为0）
        private int newLineNumber;      // 在新文本中的行号（对于DELETE为0）
        
        public DiffLine(DiffType type, String content, int originalLineNumber, int newLineNumber) {
            this.type = type;
            this.content = content;
            this.originalLineNumber = originalLineNumber;
            this.newLineNumber = newLineNumber;
        }
        
        public DiffType getType() { return type; }
        public String getContent() { return content; }
        public int getOriginalLineNumber() { return originalLineNumber; }
        public int getNewLineNumber() { return newLineNumber; }
    }
    
    /**
     * 3-Way Merge 结果封装
     */
    public static class MergeResult {
        private String mergedContent;
        private boolean hasConflict;
        private List<String> conflicts; // 冲突详情简单描述，可选

        public MergeResult(String mergedContent, boolean hasConflict) {
            this.mergedContent = mergedContent;
            this.hasConflict = hasConflict;
            this.conflicts = new ArrayList<>();
        }

        public String getMergedContent() { return mergedContent; }
        public boolean isHasConflict() { return hasConflict; }
        public List<String> getConflicts() { return conflicts; }
    }

    /**
     * 计算两个文本的差异 (Base -> New)
     */
    public static List<DiffLine> diff(String text1, String text2) {
        List<String> lines1 = splitLines(text1);
        List<String> lines2 = splitLines(text2);
        return diffLines(lines1, lines2);
    }
    
    /**
     * 三路合并算法
     * @param baseStr 基础版本（共同祖先）
     * @param oursStr 当前版本（Server/Master）
     * @param theirsStr 传入版本（Incoming/Draft）
     * @return 合并结果
     */
    public static MergeResult merge(String baseStr, String oursStr, String theirsStr) {
        // 1. 预处理：如果因为某种原因 base 为空或 null，视为从空文件开始
        if (baseStr == null) baseStr = "";
        if (oursStr == null) oursStr = "";
        if (theirsStr == null) theirsStr = "";

        // 简易优化：如果一方没变，直接返回另一方
        if (baseStr.equals(oursStr)) return new MergeResult(theirsStr, false);
        if (baseStr.equals(theirsStr)) return new MergeResult(oursStr, false);
        if (oursStr.equals(theirsStr)) return new MergeResult(oursStr, false);

        List<String> base = splitLines(baseStr);
        List<String> ours = splitLines(oursStr);
        List<String> theirs = splitLines(theirsStr);

        // 2. 将 Ours 和 Theirs 分别与 Base 进行 Diff，获取 Chunk 列表
        // 注意：我们需要一种更高级的 Chunk 结构来表示变更块，而不仅仅是行列表
        // 这里为了简化实现，直接使用一种基于行匹配的合并策略
        
        // 我们定义 "Hunk" 为一组变更。
        // 为了做准确的 3-way merge，我们通常需要对齐所有行。
        // 简化算法思路：
        // 1. 计算 Diff(Base, Ours) 和 Diff(Base, Theirs)
        // 2. 将这些 Diff 映射回 Base 的行索引
        // 3. 遍历 Base 的每一行（以及由此衍生的插入行），决定取哪边的内容

        // 生成详细的 Edit Script
        List<Edit> editsOurs = computeEdits(base, ours);
        List<Edit> editsTheirs = computeEdits(base, theirs);

        return mergeEdits(base, editsOurs, editsTheirs);
    }

    // ================= 内部类与辅助方法 =================

    private static class Edit {
        DiffType type;
        List<String> lines; // 涉及的内容
        int baseStart; // 在 Base 中的起始行索引（包含）
        int baseEnd;   // 在 Base 中的结束行索引（不包含）
        
        // 对于 INSERT，baseStart = baseEnd = 插入点
        // 对于 DELETE，lines 为空
        // 对于 REPLACE (Delete + Insert)，可以表示为 Delete
        
        public Edit(DiffType type, List<String> lines, int baseStart, int baseEnd) {
            this.type = type;
            this.lines = lines;
            this.baseStart = baseStart;
            this.baseEnd = baseEnd;
        }
    }

    /**
     * 将 Diff 结果转换为相对于 Base 的 Edit 操作列表
     * 注意：这里把连续的相同类型 DiffLine 合并为一个 Edit
     */
    private static List<Edit> computeEdits(List<String> base, List<String> target) {
        List<DiffLine> diffs = diffLines(base, target);
        List<Edit> edits = new ArrayList<>();
        
        int currentBaseLine = 0;
        
        for (int i = 0; i < diffs.size(); ) {
            DiffLine line = diffs.get(i);
            
            // 跳过 EQUAL
            if (line.type == DiffType.EQUAL) {
                currentBaseLine++;
                i++;
                continue;
            }
            
            // 收集连续的变更
            int startIdx = i;
            DiffType chunkType = line.type;
            List<String> chunkLines = new ArrayList<>();
            int chunkBaseStart = currentBaseLine;
            int chunkBaseCount = 0;
            
            // 简单的贪婪合并：处理连续的 Insert 或 Delete
            // 注意：标准的 Diff 算法输出可能是交错的（Delete then Insert = Change）
            // 我们尝试检测 "Change" (替换)：即同一个位置既有 Delete 又有 Insert
            
            // 收集一段连续的非 Equal 区域
            while (i < diffs.size() && diffs.get(i).type != DiffType.EQUAL) {
                DiffLine curr = diffs.get(i);
                if (curr.type == DiffType.DELETE) {
                    currentBaseLine++; // Base 指针前进
                    chunkBaseCount++;
                } else if (curr.type == DiffType.INSERT) {
                    chunkLines.add(curr.content);
                }
                i++;
            }
            
            edits.add(new Edit(
                (chunkBaseCount > 0 && !chunkLines.isEmpty()) ? DiffType.INSERT : chunkType, // 实际上我们需要区分 Replace
                chunkLines, 
                chunkBaseStart, 
                chunkBaseStart + chunkBaseCount
            ));
        }
        return edits;
    }

    /**
     * 核心合并逻辑
     */
    private static MergeResult mergeEdits(List<String> base, List<Edit> editsOurs, List<Edit> editsTheirs) {
        List<String> resultLines = new ArrayList<>();
        boolean hasConflict = false;
        
        // 我们需要一个游标 traversing Base
        int baseIdx = 0;
        int maxBase = base.size();
        
        int idxOurs = 0;
        int idxTheirs = 0;
        
        while (baseIdx < maxBase || idxOurs < editsOurs.size() || idxTheirs < editsTheirs.size()) {
            // 获取当前 Base 位置相关的 Edit
            Edit editO = (idxOurs < editsOurs.size() && editsOurs.get(idxOurs).baseStart == baseIdx) 
                        ? editsOurs.get(idxOurs) : null;
            Edit editT = (idxTheirs < editsTheirs.size() && editsTheirs.get(idxTheirs).baseStart == baseIdx) 
                        ? editsTheirs.get(idxTheirs) : null;

            if (editO == null && editT == null) {
                // 此时双方都没有修改 baseIdx 这一行
                if (baseIdx < maxBase) {
                    resultLines.add(base.get(baseIdx));
                    baseIdx++;
                } else {
                    // base 结束了，且没有 edits，跳出
                    break;
                }
            } else if (editO != null && editT == null) {
                // 只有 Ours 修改
                resultLines.addAll(editO.lines); // 如果是 Delete，lines为空，相当于删除了
                baseIdx = editO.baseEnd; // 跳过 Base 中被修改的行
                idxOurs++;
            } else if (editO == null && editT != null) {
                // 只有 Theirs 修改
                resultLines.addAll(editT.lines);
                baseIdx = editT.baseEnd;
                idxTheirs++;
            } else {
                // 双方都在此位置有修改 (Conflict Potential)
                
                // 1. 检查修改范围是否一致（简化的逻辑）
                // 如果 baseEnd 不同，说明修改的区域大小不同，视为冲突
                // 如果内容 lines 相同，视为 Clean Merge (Same Change)
                
                if (editO.baseEnd == editT.baseEnd && listEquals(editO.lines, editT.lines)) {
                    // 双方做了完全相同的修改 -> Auto Merge
                    resultLines.addAll(editO.lines);
                    baseIdx = editO.baseEnd;
                } else {
                    // 冲突！
                    hasConflict = true;
                    
                    // 输出冲突块
                    resultLines.add("<<<<<<< HEAD (Current Version)");
                    resultLines.addAll(editO.lines); // Ours Content
                    
                    // 如果是修改/删除，显示原始 Base 内容（可选，这里为了简洁先不显示Base）
                    
                    resultLines.add("=======");
                    resultLines.addAll(editT.lines); // Theirs Content
                    resultLines.add(">>>>>>> Incoming (Your Draft)");
                    
                    // 前进 Base 指针
                    // 取两者的最大 baseEnd，跳过受影响的 Base 区域
                    baseIdx = Math.max(editO.baseEnd, editT.baseEnd);
                }
                
                // 消耗掉这两个 edit
                idxOurs++;
                idxTheirs++;
            }
        }
        
        return new MergeResult(String.join("\n", resultLines), hasConflict);
    }
    
    private static boolean listEquals(List<String> a, List<String> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (!Objects.equals(a.get(i), b.get(i))) return false;
        }
        return true;
    }

    // ================= LCS Diff Implementation =================

    private static List<String> splitLines(String text) {
        if (text == null || text.isEmpty()) return new ArrayList<>();
        return Arrays.asList(text.split("\n", -1));
    }

    private static List<DiffLine> diffLines(List<String> lines1, List<String> lines2) {
        List<DiffLine> result = new ArrayList<>();
        int m = lines1.size();
        int n = lines2.size();
        
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
        
        int i = m, j = n;
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && lines1.get(i - 1).equals(lines2.get(j - 1))) {
                result.add(0, new DiffLine(DiffType.EQUAL, lines1.get(i - 1), i, j));
                i--; j--;
            } else if (j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j])) {
                result.add(0, new DiffLine(DiffType.INSERT, lines2.get(j - 1), 0, j));
                j--;
            } else if (i > 0) {
                result.add(0, new DiffLine(DiffType.DELETE, lines1.get(i - 1), i, 0));
                i--;
            }
        }
        return result;
    }
    
    /**
     * 简单的统计类
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
        
        public int getInsertCount() { return insertCount; }
        public int getDeleteCount() { return deleteCount; }
        public int getEqualCount() { return equalCount; }
    }
    
    public static DiffStats getStats(List<DiffLine> diffLines) {
        int i=0, d=0, e=0;
        for (DiffLine l : diffLines) {
            if (l.type == DiffType.INSERT) i++;
            else if (l.type == DiffType.DELETE) d++;
            else e++;
        }
        return new DiffStats(i, d, e);
    }
}
