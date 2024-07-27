package com.example.reactornetdisk.repository

import com.example.reactornetdisk.entity.Folder
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface FolderRepository: ReactiveCrudRepository<Folder, Long> {
    fun findByUserId(userId: Int): Flux<Folder>
    fun findByIdAndUserId(id: Long, userId: Int): Mono<Folder>
    fun findByUserIdAndParentId(userId: Int, parentId: Long?): Flux<Folder>
    fun deleteFolderByIdAndUserId(id: Long, userId: Int): Flux<Folder>
    fun deleteByIdAndUserId(id: Long, userId: Int): Mono<Boolean>
    fun findByUserIdAndParentIdAndName(userId: Int, parentId: Long?, currentFolderName: String): Mono<Folder>
    @Modifying
    @Query("UPDATE folder SET name = :name, public_flag = :publicFlag, description = :description WHERE user_id = :userId AND id = :folderId")
    fun updateFolder(userId: Int, folderId: Long, name: String, publicFlag: Boolean, description: String?): Mono<Int>
}