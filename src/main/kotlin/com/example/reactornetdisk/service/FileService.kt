package com.example.reactornetdisk.service

import com.example.reactornetdisk.entity.File
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
class FileService(
    private val folderRepository: FolderRepository,
    private val fileRepository: FileRepository,
    @Value("\${file.upload.path}") private val uploadPath: String
) {
    /**
     * 保存文件到磁盘
     */
    fun saveFiles(filePartFlux: Flux<FilePart>): Mono<String> {
        val localDate = LocalDate.now()
        val datePath = UploadUtil.getDatePath(java.io.File.separator, localDate)
        val baseDir = Paths.get(uploadPath, datePath)

        return filePartFlux.flatMap { part ->
            val filename = part.filename()
            val randomName = UUID.randomUUID().toString().replace("-", "")
            val destFile = baseDir.resolve(randomName)

            Mono.fromCallable {
                Files.createDirectories(destFile.parent)
                destFile
            }.publishOn(Schedulers.boundedElastic())
                .flatMap { file ->
//                    uploadFileInChunks(part, file)
                        part.transferTo(file)
                        .then(Mono.fromCallable { Files.size(file) })
                        .flatMap { fileSize ->
                            fileRepository.save(
                                File(
                                    name = filename,
                                    pathName = UploadUtil.getDatePath("/", localDate) + "/" + randomName,
                                    userId = 1,
                                    folderId = null,
                                    size = fileSize,
                                    mimeType = part.headers().contentType?.toString()
                                )
                            )
                        }
                        .thenReturn("File uploaded successfully: $filename")
                }
        }.collectList().flatMap { results ->
            Mono.just("Uploaded ${results.size} files: ${results.joinToString(", ")}")
        }.onErrorResume { error ->
            error.printStackTrace()
            Mono.just("Error uploading files: ${error.message}")
        }
    }

    /**
     * 从数据库中获取文件信息
     */
    fun getFileById(id: Long): Mono<File> {
        return fileRepository.findById(id)
    }
}