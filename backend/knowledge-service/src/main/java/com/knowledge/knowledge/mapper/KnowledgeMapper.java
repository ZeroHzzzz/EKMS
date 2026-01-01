package com.knowledge.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.knowledge.entity.Knowledge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface KnowledgeMapper extends BaseMapper<Knowledge> {
    @Update("UPDATE knowledge SET click_count = click_count + 1 WHERE id = #{id}")
    void incrementClickCount(Long id);

    @Update("UPDATE knowledge SET collect_count = collect_count + #{delta} WHERE id = #{id}")
    void updateCollectCount(@Param("id") Long id, @Param("delta") int delta);

    @Select("SELECT COALESCE(SUM(click_count), 0) FROM knowledge")
    Long getTotalClicks();

    @Select("SELECT COALESCE(SUM(collect_count), 0) FROM knowledge")
    Long getTotalCollections();

    // 分类统计
    @Select("SELECT category, COUNT(*) as count, COALESCE(SUM(click_count), 0) as clicks, COALESCE(SUM(collect_count), 0) as collections " +
            "FROM knowledge WHERE category IS NOT NULL GROUP BY category")
    List<Map<String, Object>> getCategoryStats();

    // 最近7天的点击量趋势（按创建日期统计）
    @Select("SELECT DATE(create_time) as date, SUM(click_count) as value " +
            "FROM knowledge WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) " +
            "GROUP BY DATE(create_time) ORDER BY date")
    List<Map<String, Object>> getClickTrend();

    // 最近7天的收藏量趋势
    @Select("SELECT DATE(create_time) as date, SUM(collect_count) as value " +
            "FROM knowledge WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) " +
            "GROUP BY DATE(create_time) ORDER BY date")
    List<Map<String, Object>> getCollectTrend();
}

