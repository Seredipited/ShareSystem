package com.sharesystem.mapper;

import com.sharesystem.entity.OperationLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 操作日志Mapper
 */
public interface OperationLogMapper {

    @Select("SELECT * FROM operation_log WHERE id = #{id}")
    OperationLog selectById(Long id);

    @Select("SELECT * FROM operation_log ORDER BY create_time DESC LIMIT #{limit}")
    List<OperationLog> selectRecent(@Param("limit") int limit);

    @Select("SELECT * FROM operation_log WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<OperationLog> selectByUserId(Long userId);

    @Select("SELECT * FROM operation_log ORDER BY create_time DESC")
    List<OperationLog> selectAll();

    @Select("SELECT * FROM operation_log WHERE operation = #{operation} ORDER BY create_time DESC")
    List<OperationLog> selectByOperation(String operation);

    @Insert("INSERT INTO operation_log(user_id, username, operation, target, detail, ip) " +
            "VALUES(#{userId}, #{username}, #{operation}, #{target}, #{detail}, #{ip})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OperationLog log);
}
