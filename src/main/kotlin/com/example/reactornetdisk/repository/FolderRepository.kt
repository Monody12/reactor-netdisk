package com.example.reactornetdisk.repository

import com.example.reactornetdisk.entity.Folder
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux

interface FolderRepository: ReactiveCrudRepository<Folder, Long> {
    fun findByUserId(userId: Int): Flux<Folder>
    fun findByUserIdAndParentId(userId: Int, parentId: Long?): Flux<Folder>
    fun deleteFolderByIdAndUserId(id: Long, userId: Int): Flux<Folder>
}