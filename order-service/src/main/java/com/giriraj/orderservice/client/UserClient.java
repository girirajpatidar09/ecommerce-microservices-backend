package com.giriraj.orderservice.client;

import com.giriraj.orderservice.dto.ApiResponse;
import com.giriraj.orderservice.dto.remote.UserSummaryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "user-service",
        path = "/api/users"
)
public interface UserClient {

    @GetMapping("/{id}")
    ApiResponse<UserSummaryDTO> getUserById(
            @PathVariable("id") Long id
    );
}