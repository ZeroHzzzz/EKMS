package com.knowledge.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.knowledge.entity.Knowledge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface KnowledgeMapper extends BaseMapper<Knowledge> {
    @Update("UPDATE knowledge SET click_count = click_count + 1 WHERE id = #{id}")
    void incrementClickCount(Long id);

    @Update("UPDATE knowledge SET collect_count = collect_count + #{delta} WHERE id = #{id}")
    void updateCollectCount(Long id, int delta);
}

