package com.knowledge.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.file.entity.ChunkInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChunkInfoMapper extends BaseMapper<ChunkInfo> {
    @Select("SELECT * FROM chunk_info WHERE upload_id = #{uploadId} ORDER BY chunk_index")
    List<ChunkInfo> selectByUploadId(String uploadId);
}

