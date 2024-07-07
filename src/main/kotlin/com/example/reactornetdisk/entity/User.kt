package com.example.reactornetdisk.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("user")
data class User(
    @Id val id: Long? = null,
    val username: String,
    var password: String,
    var email: String? = null,
    val createTime : LocalDateTime? = null,
    val updateTime : LocalDateTime? = null,
    val deleteFlag: Boolean? = null
)
