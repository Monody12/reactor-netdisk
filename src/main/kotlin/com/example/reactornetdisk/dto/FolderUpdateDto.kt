package com.example.reactornetdisk.dto

data class FolderUpdateDto(
    val id: Long,
    val name: String,
    val publicFlag: Boolean,
    val description: String?
)
