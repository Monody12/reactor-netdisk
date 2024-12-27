package com.example.reactornetdisk.entity

import org.springframework.data.relational.core.mapping.Table

@Table("file")
class File(
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
     * 是否公开
     */
    var publicFlag : Boolean,
    /**
     * 文件描述
     */
    var description: String?,
    /**
     * 文件存储路径
     */
    var pathName: String,
    /**
     * 文件大小，单位字节
     */
    var size: Long,
    var mimeType: String?
) : BaseFile(isFolder = false) {

    override fun toString(): String {
        return "File(userId=$userId, folderId=$folderId, name='$name', publicFlag=$publicFlag, description=$description, pathName='$pathName', size=$size, mimeType=$mimeType)"
    }
}