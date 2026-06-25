package com.sharesystem.mapper;

import com.sharesystem.entity.Share;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 分享Mapper
 */
public interface ShareMapper {

    @Select("SELECT * FROM share WHERE id = #{id}")
    Share selectById(Long id);

    @Select("SELECT * FROM share WHERE share_code = #{shareCode}")
    Share selectByShareCode(String shareCode);

    @Select("SELECT * FROM share WHERE file_id = #{fileId}")
    Share selectByFileId(Long fileId);

    @Select("SELECT * FROM share WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<Share> selectByUserId(Long userId);

    @Insert("INSERT INTO share(file_id, user_id, share_code, share_pwd, expire_time) " +
            "VALUES(#{fileId}, #{userId}, #{shareCode}, #{sharePwd}, #{expireTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Share share);

    @Update("UPDATE share SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(Long id);

    @Update("UPDATE share SET download_count = download_count + 1 WHERE id = #{id}")
    int incrementDownloadCount(Long id);

    @Delete("DELETE FROM share WHERE id = #{id}")
    int deleteById(Long id);

    @Delete("DELETE FROM share WHERE file_id = #{fileId}")
    int deleteByFileId(Long fileId);
}
