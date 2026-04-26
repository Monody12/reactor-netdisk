package com.example.reactornetdisk.dto

/**
 * DTO for folder upload request
 */
data class FolderUploadRequest(
    val parentId: Long? = null,
    val publicFlag: Boolean = false
)
