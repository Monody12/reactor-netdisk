package com.example.reactornetdisk.service

import com.example.reactornetdisk.entity.File
import com.example.reactornetdisk.entity.FileToken
import com.example.reactornetdisk.entity.Folder
import com.example.reactornetdisk.exception.FolderNotFoundException
import com.example.reactornetdisk.repository.FileRepository
import com.example.reactornetdisk.repository.FileTokenRepository
import com.example.reactornetdisk.repository.FolderRepository
import com.example.reactornetdisk.util.UploadUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.toFlux
import java.io.FileNotFoundException
import java.nio.channels.AsynchronousFileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

@Service
class FileService(
    private val folderRepository: FolderRepository,
    private val fileRepository: FileRepository,
    private val fileTokenRepository: FileTokenRepository,
    @Value("\${file.upload.path}") private val uploadPath: String
) {
    /**
     * 保存文件到磁盘
     */
    fun saveFiles(
        filePartFlux: Flux<FilePart>,
        folderId: Long? = null,
        userId: Int
    ): Flux<File> {
        val localDate = LocalDate.now()
        val datePath = UploadUtil.getDatePath(java.io.File.separator, localDate)
        val baseDir = Paths.get(uploadPath, datePath)

        return Mono.justOrEmpty(folderId)
            .flatMap { id ->
                folderRepository.findById(id)
                    .switchIfEmpty(Mono.error(FolderNotFoundException()))
            }
            .defaultIfEmpty(Folder()) // 假设 Folder 类有一个无参构造函数
            .flatMapMany { _ -> filePartFlux }
            .flatMap { part ->
                val filename = part.filename()
                val randomName = UUID.randomUUID().toString().replace("-", "")
                val destFile = baseDir.resolve(randomName)

                Mono.fromCallable {
                    Files.createDirectories(destFile.parent)
                    destFile
                }.publishOn(Schedulers.boundedElastic())
                    .flatMap { file ->
                        part.transferTo(file)
                            .then(Mono.fromCallable { Files.size(file) })
                            .flatMap { fileSize ->
                                val insertFile = File(
                                    name = filename,
                                    pathName = UploadUtil.getDatePath("/", localDate) + "/" + randomName,
                                    userId = userId,
                                    folderId = folderId,
                                    size = fileSize,
                                    mimeType = part.headers().contentType?.toString(),
                                    description = null
                                )
                                fileRepository.save(insertFile)
                            }
                    }
            }
            .onErrorResume { error ->
                println("Error uploading files: ${error.message}")
                error.printStackTrace()
                Flux.empty()
            }
    }


    /**
     * 从数据库中获取文件信息
     */
    fun getFileById(id: Long): Mono<File> {
        return fileRepository.findById(id)
    }

    /**
     * 删除文件
     * 包含从数据库中删除和从磁盘中删除
     */
    fun deleteFile(userId: Int, id: Long): Mono<Void> {
        return fileRepository.findById(id)
            .filter { it.userId == userId }
            .flatMap { file ->
                val path = Paths.get(uploadPath, file.pathName)
                Mono.fromCallable {
                    Files.delete(path)
                }.publishOn(Schedulers.boundedElastic())
                    .then(fileRepository.deleteById(id))
            }
            .onErrorResume { error ->
                error.printStackTrace()
                Mono.error(error)
            }
    }

    fun applyFileToken(userId: Int, fileId: Long): Mono<FileToken> {
        // 用户申请文件访问token，要校验文件所有者
        return fileRepository.findById(fileId)
            .flatMap { fileInfo ->
                if (fileInfo.userId != userId) {
                    Mono.error(FileNotFoundException())
                } else {
                    val fileToken = FileToken(
                        fileId = fileId,
                        token = UUID.randomUUID().toString().replace("-", ""),
                        expireAt = LocalDateTime.now().plusDays(7)
                    )
                    fileTokenRepository.save(fileToken)
                }
            }

    }

}