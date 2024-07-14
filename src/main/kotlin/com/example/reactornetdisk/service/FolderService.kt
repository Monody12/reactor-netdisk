package com.example.reactornetdisk.service

import com.example.reactornetdisk.entity.BaseFile
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
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@Service
class FolderService(
    private val folderRepository: FolderRepository,
    private val fileRepository: FileRepository,
    @Value("\${file.upload.path}") private val uploadPath: String
) {
    /**
     * 创建文件夹
     */
    fun createFolder(userId: Int, parentId: Long?, folderName: String): Mono<Folder> {
        return folderRepository.save(
            Folder(
                userId = userId,
                parentId = parentId,
                name = folderName
            )
        )
    }

    /**
     * 从路径字符串获取文件夹ID
     * 例如输入："/测试1/测试2"
     */
    fun getFolderIdFromPath(userId: Int, path: String): Mono<Long> {
        val folderNames = path.trim('/').split('/').filter { it.isNotEmpty() }
        return if (folderNames.isEmpty()) {
            Mono.empty()
        } else {
            findFolderIdRecursively(userId, null, folderNames)
        }
    }

    private fun findFolderIdRecursively(userId: Int, parentId: Long?, folderNames: List<String>): Mono<Long> {
        if (folderNames.isEmpty()) {
            return if (parentId == null) { Mono.empty() } else { Mono.just(parentId) }
        }

        val currentFolderName = folderNames.first()

        return folderRepository.findByUserIdAndParentIdAndName(userId, parentId, currentFolderName)
            .flatMap { folder ->
                findFolderIdRecursively(userId, folder.id, folderNames.drop(1))
            }
            .switchIfEmpty(Mono.error(NoSuchElementException("Folder not found: $currentFolderName")))
    }

    /**
     * 获取指定文件夹中的文件和文件夹
     */
    fun getFilesAndFolders(userId: Int, parentId: Long?, publicFlag: Boolean): Flux<BaseFile> {
        val folderFlux: Flux<Folder> = folderRepository.findByUserIdAndParentId(userId, parentId)
        val fileFlux: Flux<File> = if (publicFlag) {
            fileRepository.findByUserIdAndFolderIdAndPublicFlagIsTrue(userId, parentId)
        } else {
            fileRepository.findByUserIdAndFolderId(userId, parentId)
        }
        // 将两个 Flux 合并为一个 Flux
        return Flux.concat(folderFlux, fileFlux)
    }


    /**
     * 删除文件夹及其下所有文件和文件夹
     * @param userId 用户ID，确保只删除当前用户的文件和文件夹
     * @param folderIdList 文件夹ID列表
     * @param fileIdList 文件ID列表
     * @return 删除结果（共删除?个文件，?个文件夹）
     */
    fun deleteFileAndFolder(
        userId: Int,
        fileIdList: List<Long>?,
        folderIdList: List<Long>?
    ): Mono<String> {
        val deletedFileCount = AtomicInteger(0)
        val deletedFolderCount = AtomicInteger(0)

        return Mono.defer {
            // 删除选中的文件
            val fileDeletions = deleteFiles(userId, fileIdList, deletedFileCount)
            // 再删除选中的文件夹
            val folderDeletions = deleteFoldersRecursively(userId, folderIdList, deletedFileCount, deletedFolderCount)

            Mono.`when`(fileDeletions, folderDeletions)
                .then(Mono.fromCallable {
                    "共删除${deletedFileCount.get()}个文件，${deletedFolderCount.get()}个文件夹"
                })
        }
    }

    private fun deleteFiles(
        userId: Int,
        fileIdList: List<Long>?,
        deletedFileCount: AtomicInteger
    ): Mono<Void> {
        return if (fileIdList.isNullOrEmpty()) {
            Mono.empty()
        } else {
            Flux.fromIterable(fileIdList)
                .flatMap { fileId ->
                    fileRepository.findById(fileId)
                        // 确保只删除当前用户的文件
                        .filter { it.userId == userId }
                        .flatMap { file ->
                            deleteFileFromDisk(file.pathName)
                                .then(fileRepository.deleteById(fileId))
                                .doOnSuccess { deletedFileCount.incrementAndGet() }
                        }
                }
                .then()
        }
    }

    private fun deleteFoldersRecursively(
        userId: Int,
        folderIdList: List<Long>?,
        deletedFileCount: AtomicInteger,
        deletedFolderCount: AtomicInteger
    ): Mono<Void> {
        if (folderIdList.isNullOrEmpty()) {
            return Mono.empty()
        }

        return Flux.fromIterable(folderIdList)
            // 如果文件夹不存在，跳过这个文件夹的删除操作
            .flatMap {
                folderId-> folderRepository.findByIdAndUserId(folderId, userId)
                .flatMap { folder ->
                    deleteFolderContents(userId, folder.id!!, deletedFileCount, deletedFolderCount)
                        .then(folderRepository.deleteById(folder.id!!))
                        .doOnSuccess { deletedFolderCount.incrementAndGet() }
                }
            }
            .onErrorResume { error ->
                println("Error deleting folder: ${error.message}")
                Mono.empty()
            }
            .then()
    }

    private fun deleteFolderContents(
        userId: Int,
        folderId: Long,
        deletedFileCount: AtomicInteger,
        deletedFolderCount: AtomicInteger
    ): Mono<Void> {
        // 删除当前文件夹下的文件
        val deleteFiles = fileRepository.findByUserIdAndFolderId(userId, folderId)
            .flatMap { file ->
                deleteFileFromDisk(file.pathName)
                    .then(fileRepository.deleteById(file.id!!))
                    .doOnSuccess { deletedFileCount.incrementAndGet() }
            }

        // 删除当前文件夹下的子文件夹
        val deleteSubFolders = folderRepository.findByUserIdAndParentId(userId, folderId)
            .flatMap { folder ->
                // 递归调用
                deleteFolderContents(userId, folder.id!!, deletedFileCount, deletedFolderCount)
                    .then(folderRepository.deleteByIdAndUserId(folderId,userId))
                    .doOnSuccess { deletedFolderCount.incrementAndGet() }
            }

        return Flux.merge(deleteFiles, deleteSubFolders).then()
    }

    private fun deleteFileFromDisk(pathName: String): Mono<Void> {
        return Mono.fromRunnable {
            try {
                Files.deleteIfExists(Paths.get(uploadPath, pathName))
            } catch (e: Exception) {
                e.printStackTrace()
                // 在实际应用中，您可能希望记录这个错误，或者以某种方式处理它
                println("Failed to delete file: $pathName. Error: ${e.message}")
            }
        }
    }

}