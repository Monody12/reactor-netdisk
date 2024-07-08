package com.example.reactornetdisk.entity

import org.springframework.data.relational.core.mapping.Table

@Table("folder")
class Folder(
    var userId: Int,
    var parentId: Long?,
    var name: String
):BaseFile()