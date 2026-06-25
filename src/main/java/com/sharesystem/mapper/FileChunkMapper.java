package com.sharesystem.mapper;

import com.sharesystem.entity.FileChunk;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 文件分片Mapper（分片上传）
 */
public interface FileChunkMapper {

    @Select("SELECT * FROM file_chunk WHERE id = #{id}")
    FileChunk selectById(Long id);

    @Select("SELECT * FROM file_chunk WHERE file_md5 = #{fileMd5} ORDER BY chunk_index ASC")
    List<FileChunk> selectByFileMd5(String fileMd5);

    @Select("SELECT * FROM file_chunk WHERE file_md5 = #{fileMd5} AND chunk_index = #{chunkIndex}")
    FileChunk selectByMd5AndIndex(@Param("fileMd5") String fileMd5, @Param("chunkIndex") Integer chunkIndex);

    @Select("SELECT COUNT(*) FROM file_chunk WHERE file_md5 = #{fileMd5}")
    int countByFileMd5(String fileMd5);

    @Insert("INSERT INTO file_chunk(file_md5, chunk_index, chunk_md5, chunk_size, chunk_path, total_chunks, file_name, user_id) " +
            "VALUES(#{fileMd5}, #{chunkIndex}, #{chunkMd5}, #{chunkSize}, #{chunkPath}, #{totalChunks}, #{fileName}, #{userId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FileChunk chunk);

    @Delete("DELETE FROM file_chunk WHERE file_md5 = #{fileMd5}")
    int deleteByFileMd5(String fileMd5);

    @Delete("DELETE FROM file_chunk WHERE file_md5 = #{fileMd5} AND user_id = #{userId}")
    int deleteByMd5AndUser(@Param("fileMd5") String fileMd5, @Param("userId") Long userId);

    @Select("SELECT DISTINCT chunk_index FROM file_chunk WHERE file_md5 = #{fileMd5} ORDER BY chunk_index")
    List<Integer> selectUploadedChunkIndexes(String fileMd5);
}
