package com.example.reactornetdisk.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("user_token")
data class UserToken(
    @Id var id: Long? = null,
    var userId: Int?,
    var token: String?,
    var expireAt: LocalDateTime? = LocalDateTime.now().plusDays(7),
    var createdAt: LocalDateTime? = null,
    var updatedAt: LocalDateTime? = null
)
