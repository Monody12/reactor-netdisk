package com.example.reactornetdisk.dto

import com.example.reactornetdisk.entity.User

data class UserWithToken(
    val user: User,
    val token: String
)
