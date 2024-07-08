package com.example.reactornetdisk.repository

import com.example.reactornetdisk.entity.File
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface FileRepository: ReactiveCrudRepository<File, Long> {
    fun findByUserIdAndFolderId(userId: Int, folderId: Long?): Flux<File>
    fun deleteByUserIdAndId(userId: Int, id: Long): Mono<Void>
}