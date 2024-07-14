package com.example.reactornetdisk.controller

import com.example.reactornetdisk.dto.FolderDTO
import com.example.reactornetdisk.entity.ApiResponse
import com.example.reactornetdisk.entity.BaseFile
import com.example.reactornetdisk.entity.Folder
import com.example.reactornetdisk.exception.FileNotFoundInDatabaseException
import com.example.reactornetdisk.service.FolderService
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

@RestController
@RequestMapping("/api/folder")
class FolderController(
    private val folderService: FolderService
) {
    /**
     * 创建文件夹
     */
    @PostMapping
    fun createFolder(
        @RequestBody folderDto: FolderDTO,
        exchange: ServerWebExchange
    ): Mono<ApiResponse<Folder>> {
        return folderService.createFolder(
            userId = exchange.attributes["userId"] as Int,
            parentId = folderDto.parentId,
            folderName = folderDto.name
        ).map { ApiResponse(200, "创建成功", it) }
    }

    /**
     * 获取指定文件夹中的文件和文件夹
     * 父文件夹id与路径字符串二选一，如果都不传则访问根目录
     */
    @GetMapping
    fun getFilesAndFolders(
        @RequestParam(required = false) parentId: Long?,
        @RequestParam(required = false) folderPath: String?,
        @RequestParam(required = false, defaultValue = "false") publicFlag: Boolean,
        exchange: ServerWebExchange
    ): Mono<ApiResponse<List<BaseFile>>> {
        val userId = exchange.attributes["userId"] as Int
        if (folderPath == null) {
            return folderService.getFilesAndFolders(userId, parentId, publicFlag)
                .collectList()
                .map { files -> ApiResponse(code = 200, msg = "查询成功", data = files) }
        }
        if (folderPath.trim().split("/").none { it.isNotBlank() }){
            return folderService.getFilesAndFolders(userId, null, publicFlag)
                .collectList()
                .map { files -> ApiResponse(code = 200, msg = "null", data = files) }
        }
        return folderService.getFolderIdFromPath(userId = userId, path = folderPath)
            .switchIfEmpty { Mono.error(FileNotFoundInDatabaseException()) }
            .flatMap { folderId ->
                folderService.getFilesAndFolders(userId, folderId, publicFlag)
                    .collectList()
                    .map { files -> ApiResponse(code = 200, msg = folderId.toString(), data = files) }
            }
    }

    /**
     * 删除文件夹及其下所有文件和文件夹
     * @param folderIdList 文件夹ID列表
     * @param fileIdList 文件ID列表
     * @return 删除结果（共删除?个文件，?个文件夹）
     */
    @DeleteMapping
    fun deleteFileAndFolder(
        @RequestParam(required = false) fileIdList: List<Long>?,
        @RequestParam(required = false) folderIdList: List<Long>?,
        exchange: ServerWebExchange
    ): Mono<ApiResponse<String>> {
        return folderService.deleteFileAndFolder(
            userId = exchange.attributes["userId"] as Int,
            fileIdList = fileIdList,
            folderIdList = folderIdList
        ).map { ApiResponse(200, it, null) }
    }

}