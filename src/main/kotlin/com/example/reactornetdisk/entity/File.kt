package com.example.reactornetdisk.entity

import org.springframework.data.relational.core.mapping.Table

@Table("file")
class File (
    override var userId: Int,
    /**
     * 文件夹ID，如果为null则表示在根目录
     */
    val folderId: Long?,
    /**
     * 上传时的文件名
     */
    val name: String,
    /**
     * 文件存储路径
     */
    val pathName: String,
    /**
     * 文件大小，单位字节
     */
    val size: Long,
    val mimeType: String?
): BaseFile()