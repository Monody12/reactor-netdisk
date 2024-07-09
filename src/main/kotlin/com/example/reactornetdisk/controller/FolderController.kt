package com.example.reactornetdisk.controller

import com.example.reactornetdisk.dto.FolderDto
import com.example.reactornetdisk.entity.ApiResponse
import com.example.reactornetdisk.entity.BaseFile
import com.example.reactornetdisk.entity.Folder
import com.example.reactornetdisk.service.FolderService
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/folder")
class FolderController(
    private val folderService: FolderService
) {
    /**
     * 创建文件夹
     */
    @PostMapping
    fun createFolder(@RequestBody folderDto: FolderDto) : Mono<ApiResponse<Folder>> {
        return folderService.createFolder(
            userId = 1,
            parentId = folderDto.parentId,
            folderName = folderDto.name
        ).map { ApiResponse(200,"创建成功", it) }
    }

    /**
     * 获取指定文件夹中的文件和文件夹
     */
    @GetMapping
    fun getFilesAndFolders(@RequestParam(required = false) parentId: Long?): Mono<ApiResponse<List<BaseFile>>> {
        return folderService.getFilesAndFolders(1, parentId).collectList()
            .map { files -> ApiResponse(code = 200, msg = "查询成功", data = files) }
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
        @RequestParam(required = false) folderIdList: List<Long>?
    ): Mono<ApiResponse<String>> {
        return folderService.deleteFileAndFolder(
            userId = 1,
            fileIdList = fileIdList,
            folderIdList = folderIdList
        ).map { ApiResponse(200, it, null) }
    }

}