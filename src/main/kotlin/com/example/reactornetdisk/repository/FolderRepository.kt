package com.example.reactornetdisk.repository

import com.example.reactornetdisk.entity.Folder
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux

interface FolderRepository: ReactiveCrudRepository<Folder, Long> {
    fun findByUserId(userId: Long): Flux<Folder>
    fun findByUserIdAndParentId(userId: Long, parentId: Long?): Flux<Folder>
    fun deleteFolderByIdAndUserId(id: Long, userId: Long): Flux<Folder>
}