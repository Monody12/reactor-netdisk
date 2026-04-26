package com.example.reactornetdisk.dto

import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * DTO for user login
 */
data class UserDTO(
    @field:Size(min = 3, max = 18, message = "用户名需要3-18位")
    @field:Pattern(regexp = "^\\w+$", message = "用户名只能包含字母、数字和下划线")
    val username: String,

    @field:Size(min = 3, max = 18, message = "密码需要3-18位")
    @field:Pattern(regexp = "^[A-Za-z0-9]+$", message = "密码只能包含字母和数字")
    val password: String,

    val email: String?
)
