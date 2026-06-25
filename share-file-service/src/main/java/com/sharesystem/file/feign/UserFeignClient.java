package com.sharesystem.file.feign;

import com.sharesystem.common.dto.R;
import com.sharesystem.common.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * User Service Feign 客户端
 */
@FeignClient(name = "share-user-service", url = "http://localhost:8181", path = "/api/user")
public interface UserFeignClient {

    @GetMapping("/internal/{userId}")
    R<UserDTO> getUserById(@PathVariable Long userId);

    @GetMapping("/internal/count")
    R<Integer> getUserCount();}
