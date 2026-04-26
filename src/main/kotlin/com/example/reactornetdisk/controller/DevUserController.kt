package com.example.reactornetdisk.controller

import com.example.reactornetdisk.dto.UserDTO
import com.example.reactornetdisk.entity.ApiResponse
import com.example.reactornetdisk.service.UserService
import jakarta.validation.Valid
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/user")
@Profile("dev")
class DevUserController(private val userService: UserService) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody userDTO: UserDTO): Mono<ApiResponse<Unit>> {
        return userService.createUser(userDTO)
            .map<ApiResponse<Unit>> { ApiResponse(200, "注册成功", null) }
            .onErrorResume { ex ->
                if (ex.message?.contains("23505") == true || ex.message?.contains("username") == true) {
                    Mono.just(ApiResponse<Unit>(400, "用户名已存在", null))
                } else {
                    Mono.just(ApiResponse<Unit>(500, "注册失败: ${ex.message}", null))
                }
            }
    }
}
