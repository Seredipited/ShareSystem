package com.sharesystem.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sharesystem.common.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {

    @Select("SELECT * FROM operation_log ORDER BY create_time DESC LIMIT #{limit}")
    List<OperationLog> selectRecent(int limit);

    @Select("SELECT * FROM operation_log WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<OperationLog> selectByUserId(Long userId);

    @Select("SELECT * FROM operation_log ORDER BY create_time DESC")
    List<OperationLog> selectAll();
}
