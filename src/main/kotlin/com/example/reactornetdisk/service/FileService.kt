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
import reactor.kotlin.core.publisher.toMono
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
import java.util.concurrent.atomic.AtomicReference

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
        publicFlag: Boolean = false,
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
                                    publicFlag = publicFlag,
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

    fun applyFileTokenAndGetDownloadString(userId: Int, fileIdList: List<Long>): Flux<String> {
        // 用户申请文件访问token，要校验文件所有者
        val token :String = UUID.randomUUID().toString().replace("-", "")
        return fileRepository.findIdByUserIdAndFileIdIn(userId, fileIdList)
            // 生成FileToken对象
            .flatMap { filterFileId ->
                Mono.just(FileToken(fileId = filterFileId, token = token))
            }
            .toFlux()
            .collectList()
            .flatMapMany { fileTokenRepository.saveAll(it) }
            .flatMap { fileToken ->
                 Mono.just("/api/files/download?fileId=${fileToken.fileId}&token=${fileToken.token}")
            }
    }

    /**
     * 递归上传文件夹（支持带相对路径的文件批量上传）
     * @param filePartsWithPath 文件列表，每个文件包含FilePart和相对路径
     * @param parentId 父文件夹ID，null表示根目录
     * @param publicFlag 是否公开
     * @param userId 用户ID
     * @return 上传成功的文件列表
     */
    fun saveFilesWithFolderStructure(
        filePartsWithPath: Flux<Pair<org.springframework.http.codec.multipart.FilePart, String>>,
        parentId: Long?,
        publicFlag: Boolean,
        userId: Int
    ): Flux<File> {
        val localDate = LocalDate.now()
        val datePath = UploadUtil.getDatePath(java.io.File.separator, localDate)
        val baseDir = Paths.get(uploadPath, datePath)

        // 缓存已创建的文件夹映射：路径 -> folderId
        val folderCache = AtomicReference<Map<String, Long>>(emptyMap())

        // 首先验证父文件夹存在
        return Mono.justOrEmpty(parentId)
            .flatMap { id ->
                folderRepository.findById(id)
                    .switchIfEmpty(Mono.error(FolderNotFoundException()))
            }
            .defaultIfEmpty(Folder())
            .thenMany(filePartsWithPath)
            .flatMap { (filePart, relativePath) ->
                val pathParts = relativePath.split("/").filter { it.isNotEmpty() }
                if (pathParts.isEmpty()) {
                    // 没有路径信息，直接上传到父文件夹
                    saveSingleFile(filePart, parentId, publicFlag, userId, baseDir, localDate)
                } else {
                    // 解析路径，创建必要的文件夹，然后上传文件
                    val folderNames = pathParts.dropLast(1) // 除了文件名之外的路径部分
                    val fileName = pathParts.last()

                    createFoldersRecursively(userId, parentId, folderNames, folderCache)
                        .flatMap { resolvedFolderId ->
                            // 修改文件名为原始文件名但保存到目标文件夹
                            saveSingleFileWithName(filePart, fileName, resolvedFolderId, publicFlag, userId, baseDir, localDate)
                        }
                }
            }
            .onErrorResume { error ->
                println("Error uploading files with folder structure: ${error.message}")
                error.printStackTrace()
                Flux.empty()
            }
    }

    /**
     * 递归创建文件夹结构
     */
    private fun createFoldersRecursively(
        userId: Int,
        parentId: Long?,
        folderNames: List<String>,
        folderCache: AtomicReference<Map<String, Long>>
    ): Mono<Long> {
        if (folderNames.isEmpty()) {
            return Mono.justOrEmpty(parentId).defaultIfEmpty(0L)
        }

        // 构建路径键，用于缓存
        val parentKey = parentId?.toString() ?: "root"
        val currentFolderName = folderNames.first()
        val currentPathKey = "$parentKey/$currentFolderName"

        // 检查缓存
        val cachedId = folderCache.get()[currentPathKey]
        if (cachedId != null) {
            return createFoldersRecursively(userId, cachedId, folderNames.drop(1), folderCache)
        }

        return folderRepository.findByUserIdAndParentIdAndName(userId, parentId, currentFolderName)
            .flatMap { existingFolder: Folder ->
                // 文件夹已存在，更新缓存并继续递归
                val updatedCache = folderCache.get().toMutableMap()
                updatedCache[currentPathKey] = existingFolder.id!!
                folderCache.set(updatedCache)
                createFoldersRecursively(userId, existingFolder.id, folderNames.drop(1), folderCache)
            }
            .switchIfEmpty(
                Mono.defer {
                    // 创建新文件夹
                    val newFolder = Folder(
                        userId = userId,
                        parentId = parentId,
                        description = null,
                        publicFlag = false,
                        name = currentFolderName
                    )
                    folderRepository.save(newFolder)
                        .flatMap { savedFolder: Folder ->
                            // 更新缓存
                            val updatedCache = folderCache.get().toMutableMap()
                            updatedCache[currentPathKey] = savedFolder.id!!
                            folderCache.set(updatedCache)
                            createFoldersRecursively(userId, savedFolder.id, folderNames.drop(1), folderCache)
                        }
                }
            )
    }

    /**
     * 保存单个文件（保留原始文件名）
     */
    private fun saveSingleFile(
        filePart: org.springframework.http.codec.multipart.FilePart,
        folderId: Long?,
        publicFlag: Boolean,
        userId: Int,
        baseDir: Path,
        localDate: LocalDate
    ): Mono<File> {
        return saveSingleFileWithName(filePart, filePart.filename(), folderId, publicFlag, userId, baseDir, localDate)
    }

    /**
     * 保存单个文件（指定文件名）
     */
    private fun saveSingleFileWithName(
        filePart: org.springframework.http.codec.multipart.FilePart,
        fileName: String,
        folderId: Long?,
        publicFlag: Boolean,
        userId: Int,
        baseDir: Path,
        localDate: LocalDate
    ): Mono<File> {
        val randomName = UUID.randomUUID().toString().replace("-", "")
        val destFile = baseDir.resolve(randomName)

        return Mono.fromCallable {
            Files.createDirectories(destFile.parent)
            destFile
        }.publishOn(Schedulers.boundedElastic())
            .flatMap { file ->
                filePart.transferTo(file)
                    .then(Mono.fromCallable { Files.size(file) })
                    .flatMap { fileSize ->
                        val insertFile = File(
                            name = fileName,
                            publicFlag = publicFlag,
                            pathName = UploadUtil.getDatePath("/", localDate) + "/" + randomName,
                            userId = userId,
                            folderId = if (folderId == 0L) null else folderId,
                            size = fileSize,
                            mimeType = filePart.headers().contentType?.toString(),
                            description = null
                        )
                        fileRepository.save(insertFile)
                    }
            }
    }

}