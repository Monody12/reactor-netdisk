package com.example.reactornetdisk.dto

import org.springframework.http.codec.multipart.FilePart

/**
 * Wrapper for FilePart with its relative path from folder upload
 */
data class UploadFilePart(
    val filePart: FilePart,
    val relativePath: String
)
