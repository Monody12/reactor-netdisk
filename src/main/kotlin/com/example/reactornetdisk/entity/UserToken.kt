package com.example.reactornetdisk.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("user_token")
data class UserToken(
    @Id var id: Long? = null,
    var userId: Long?,
    var token: String?,
    var expireAt: LocalDateTime?,
    var createdAt: LocalDateTime?,
    var updatedAt: LocalDateTime?
)
