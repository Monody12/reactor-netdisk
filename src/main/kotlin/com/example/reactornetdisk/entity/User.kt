package com.example.reactornetdisk.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("users")
data class User(
    @Id var id: Int? = null,
    var username: String,
    var password: String,
    var email: String? = null,
    var createdAt : LocalDateTime? = null,
    var updatedAt : LocalDateTime? = null,
    var deleteFlag: Boolean? = null
)
