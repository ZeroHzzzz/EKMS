package com.knowledge.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.knowledge.entity.Knowledge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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
}

