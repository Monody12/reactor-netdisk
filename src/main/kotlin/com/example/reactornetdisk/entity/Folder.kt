package com.example.reactornetdisk.entity

import org.springframework.data.relational.core.mapping.Table

@Table("folder")
class Folder(
    var userId: Int,
    var parentId: Long?,
    var description: String?,
    var publicFlag: Boolean,
    var name: String
) : BaseFile() {
    constructor() : this(0, null, null, false, "")
}