package com.knowledge.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.audit.entity.Audit;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditMapper extends BaseMapper<Audit> {
}

