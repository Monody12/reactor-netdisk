package com.example.reactornetdisk.controller

import com.example.reactornetdisk.dto.UserDTO
import com.example.reactornetdisk.entity.ApiResponse
import com.example.reactornetdisk.entity.User
import com.example.reactornetdisk.service.UserService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/user")
class UserController(private val userService: UserService) {

    @PostMapping("/login")
    fun login(@RequestBody userDTO: UserDTO): Mono<ApiResponse<User>> =
        userService.login(userDTO)
            .map { user -> ApiResponse(200, "登录成功", user) }


    @PostMapping
    fun createUser(@RequestBody userDTO: UserDTO): Mono<ApiResponse<User>> = userService.createUser(userDTO).map { user ->
        user.password = ""
        ApiResponse(200, "用户创建成功", user)
    }
}