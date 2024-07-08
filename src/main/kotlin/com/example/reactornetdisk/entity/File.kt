package com.example.reactornetdisk.entity

import org.springframework.data.relational.core.mapping.Table

@Table("file")
class File (
    var userId: Int,
    /**
     * 文件夹ID，如果为null则表示在根目录
     */
    var folderId: Long?,
    /**
     * 上传时的文件名
     */
    var name: String,
    /**
     * 文件存储路径
     */
    var pathName: String,
    /**
     * 文件大小，单位字节
     */
    var size: Long,
    var mimeType: String?
): BaseFile()