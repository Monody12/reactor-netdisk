package com.example.reactornetdisk.repository

import com.example.reactornetdisk.entity.File
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface FileRepository : ReactiveCrudRepository<File, Long> {
    fun findByUserIdAndFolderId(userId: Int, folderId: Long?): Flux<File>
    fun findByUserIdAndFolderIdAndPublicFlagIsTrue(userId: Int, folderId: Long?): Flux<File>
    fun deleteByUserIdAndId(userId: Int, id: Long): Mono<Void>
    fun existsFileByIdAndPublicFlagIsTrue(id: Long): Mono<Boolean>
    @Query("SELECT id FROM file WHERE user_id = :userId AND id IN (:fileIds)")
    fun findIdByUserIdAndFileIdIn(userId: Int, fileIds: List<Long>): Flux<Long>
    @Modifying
    @Query("UPDATE file SET name = :name, public_flag = :publicFlag, description = :description WHERE user_id = :userId AND id = :fileId")
    fun updateFile(userId: Int, fileId: Long, name: String, publicFlag: Boolean, description: String?): Mono<Int>

}
