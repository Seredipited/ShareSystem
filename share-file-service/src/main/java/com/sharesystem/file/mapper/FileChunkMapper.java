package com.sharesystem.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sharesystem.common.entity.FileChunk;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface FileChunkMapper extends BaseMapper<FileChunk> {

    @Select("SELECT * FROM file_chunk WHERE file_md5 = #{fileMd5} AND chunk_index = #{chunkIndex}")
    FileChunk selectByMd5AndIndex(@Param("fileMd5") String fileMd5, @Param("chunkIndex") Integer chunkIndex);

    @Select("SELECT COUNT(*) FROM file_chunk WHERE file_md5 = #{fileMd5}")
    int countByFileMd5(String fileMd5);

    @Select("SELECT * FROM file_chunk WHERE file_md5 = #{fileMd5} ORDER BY chunk_index ASC")
    List<FileChunk> selectByFileMd5(String fileMd5);

    @Select("SELECT DISTINCT chunk_index FROM file_chunk WHERE file_md5 = #{fileMd5} ORDER BY chunk_index")
    List<Integer> selectUploadedChunkIndexes(String fileMd5);

    @Delete("DELETE FROM file_chunk WHERE file_md5 = #{fileMd5}")
    int deleteByFileMd5(String fileMd5);
}
