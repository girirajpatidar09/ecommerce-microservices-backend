package com.giriraj.cartservice.client;

import com.giriraj.cartservice.dto.ApiResponse;
import com.giriraj.cartservice.dto.remote.UserSummaryDTO;
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