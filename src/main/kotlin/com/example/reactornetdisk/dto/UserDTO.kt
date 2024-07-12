package com.example.reactornetdisk.dto

/**
 * DTO for user login
 */
data class UserDTO(
    val username: String,
    val password: String,
    val email: String?
)
