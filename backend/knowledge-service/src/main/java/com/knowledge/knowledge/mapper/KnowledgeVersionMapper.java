package com.knowledge.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.knowledge.entity.KnowledgeVersion;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface KnowledgeVersionMapper extends BaseMapper<KnowledgeVersion> {
    
    @org.apache.ibatis.annotations.Select("SELECT MAX(version) FROM knowledge_version WHERE knowledge_id = #{knowledgeId}")
    Long selectMaxVersion(@org.apache.ibatis.annotations.Param("knowledgeId") Long knowledgeId);
}

