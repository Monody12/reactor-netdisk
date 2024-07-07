package com.example.reactornetdisk.service

import com.example.reactornetdisk.entity.File
import com.example.reactornetdisk.entity.Folder
import com.example.reactornetdisk.repository.FileRepository
import com.example.reactornetdisk.repository.FolderRepository
import com.example.reactornetdisk.util.UploadUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.nio.channels.AsynchronousFileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

@Service
class FolderService(
    private val folderRepository: FolderRepository,
    private val fileRepository: FileRepository
) {
    /**
     * 创建文件夹
     */
    fun createFolder(userId: Int, parentId: Long?, folderName: String): Mono<Folder> {
        return folderRepository.save(Folder(
            userId = userId,
            parentId = parentId,
            name = folderName
        ))
    }

    /**
     * 获取指定文件夹中的文件和文件夹
     */
    fun getFilesAndFolders(userId: Int, parentId: Long?): Flux<Any> {
        val folderFlux : Flux<Folder> = folderRepository.findByUserIdAndParentId(userId.toLong(), parentId)
        val fileFlux : Flux<File> = fileRepository.findByUserIdAndFolderId(userId.toLong(), parentId)
        // 将两个 Flux 合并为一个 Flux
        return Flux.merge(folderFlux, fileFlux)
    }
}