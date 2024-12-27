package com.example.reactornetdisk.entity

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import java.time.LocalDateTime

open class BaseFile(
    @Id var id: Long? = null,
    var createdAt: LocalDateTime? = LocalDateTime.now(),
    var updatedAt: LocalDateTime? = LocalDateTime.now(),
    var deleteFlag: Boolean = false,
    @Transient
    var isFolder : Boolean,
) {
}
