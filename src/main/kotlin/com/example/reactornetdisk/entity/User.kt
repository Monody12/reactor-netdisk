package com.example.reactornetdisk.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("user")
data class User(
    @Id var id: Int? = null,
    var username: String,
    var password: String,
    var email: String? = null,
    var createAt : LocalDateTime? = null,
    var updateAt : LocalDateTime? = null,
    var deleteFlag: Boolean? = null
)
