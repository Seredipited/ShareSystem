package com.sharesystem.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sharesystem.common.entity.FileItem;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface FileItemMapper extends BaseMapper<FileItem> {

    @Select("SELECT * FROM file_item WHERE user_id = #{userId} AND parent_id = #{parentId} AND is_deleted = 0 ORDER BY is_folder DESC, file_name ASC")
    List<FileItem> selectByUserIdAndParent(@Param("userId") Long userId, @Param("parentId") Long parentId);

    @Select("SELECT * FROM file_item WHERE user_id = #{userId} AND is_deleted = 1 ORDER BY delete_time DESC")
    List<FileItem> selectDeletedByUserId(Long userId);

    @Select("SELECT * FROM file_item WHERE user_id = #{userId} AND is_folder = 1 AND is_deleted = 0 ORDER BY file_name ASC")
    List<FileItem> selectFoldersByUserId(Long userId);

    @Select("SELECT * FROM file_item WHERE user_id = #{userId} AND share_status = 1 AND is_deleted = 0")
    List<FileItem> selectSharedByUserId(Long userId);

    @Select("SELECT * FROM file_item WHERE file_md5 = #{md5} AND is_deleted = 0 LIMIT 1")
    FileItem selectByMd5(String md5);

    @Select("SELECT * FROM file_item WHERE user_id = #{userId} AND parent_id = #{parentId} AND file_name = #{fileName} AND is_deleted = 0 LIMIT 1")
    FileItem selectByNameAndParent(@Param("userId") Long userId, @Param("parentId") Long parentId, @Param("fileName") String fileName);

    @Select("SELECT SUM(file_size) FROM file_item WHERE user_id = #{userId} AND is_folder = 0 AND is_deleted = 0")
    Long sumFileSizeByUserId(Long userId);

    @Select("SELECT * FROM file_item WHERE user_id = #{userId} AND file_name LIKE CONCAT('%', #{keyword}, '%') AND is_deleted = 0")
    List<FileItem> searchByUserIdAndKeyword(@Param("userId") Long userId, @Param("keyword") String keyword);

    @Select("SELECT * FROM file_item WHERE is_deleted = 0 ORDER BY create_time DESC")
    List<FileItem> selectAllFiles();

    @Select("SELECT * FROM file_item WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY create_time DESC")
    List<FileItem> selectAllFilesByUserId(Long userId);

    @Update("UPDATE file_item SET is_deleted = 1, delete_time = NOW() WHERE id = #{id}")
    int softDelete(Long id);

    @Update("UPDATE file_item SET is_deleted = 0, delete_time = NULL WHERE id = #{id}")
    int restore(Long id);

    @Update("UPDATE file_item SET file_name = #{fileName} WHERE id = #{id}")
    int updateName(@Param("id") Long id, @Param("fileName") String fileName);

    @Update("UPDATE file_item SET parent_id = #{parentId} WHERE id = #{id}")
    int updateParent(@Param("id") Long id, @Param("parentId") Long parentId);

    @Update("UPDATE file_item SET share_status = #{shareStatus} WHERE id = #{id}")
    int updateShareStatus(@Param("id") Long id, @Param("shareStatus") Integer shareStatus);
}
