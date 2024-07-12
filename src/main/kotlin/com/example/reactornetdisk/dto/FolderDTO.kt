package com.example.reactornetdisk.dto

/**
 * DTO for creating a folder
 */
data class FolderDTO(
    val name: String,
    val parentId: Long? = null
)
