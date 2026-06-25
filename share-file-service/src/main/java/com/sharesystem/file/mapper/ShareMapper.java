package com.sharesystem.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sharesystem.common.entity.Share;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ShareMapper extends BaseMapper<Share> {

    @Select("SELECT * FROM share WHERE share_code = #{shareCode}")
    Share selectByShareCode(String shareCode);

    @Select("SELECT * FROM share WHERE file_id = #{fileId}")
    Share selectByFileId(Long fileId);

    @Select("SELECT * FROM share WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<Share> selectByUserId(Long userId);

    @Update("UPDATE share SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(Long id);

    @Update("UPDATE share SET download_count = download_count + 1 WHERE id = #{id}")
    int incrementDownloadCount(Long id);

    @Delete("DELETE FROM share WHERE file_id = #{fileId}")
    int deleteByFileId(Long fileId);
}
