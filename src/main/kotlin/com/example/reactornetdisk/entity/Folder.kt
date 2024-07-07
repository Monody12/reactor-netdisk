package com.example.reactornetdisk.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("folder")
data class Folder(
    @Id val id: Long? = null,
    val userId: Int,
    val parentId: Long?,
    val name: String,
    val createdAt: LocalDateTime? = LocalDateTime.now(),
    val updatedAt: LocalDateTime? = LocalDateTime.now(),
    val deleteFlag: Boolean = false
)