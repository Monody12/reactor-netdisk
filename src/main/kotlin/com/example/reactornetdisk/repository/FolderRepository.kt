package com.example.reactornetdisk.repository

import com.example.reactornetdisk.entity.Folder
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface FolderRepository: ReactiveCrudRepository<Folder, Long> {
    fun findByUserId(userId: Int): Flux<Folder>
    fun findByIdAndUserId(id: Long, userId: Int): Mono<Folder>
    fun findByUserIdAndParentId(userId: Int, parentId: Long?): Flux<Folder>
    fun deleteFolderByIdAndUserId(id: Long, userId: Int): Flux<Folder>
    fun deleteByIdAndUserId(id: Long, userId: Int): Mono<Boolean>
}