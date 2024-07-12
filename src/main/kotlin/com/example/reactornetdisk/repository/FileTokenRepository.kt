package com.example.reactornetdisk.repository

import com.example.reactornetdisk.entity.FileToken
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono

interface FileTokenRepository : ReactiveCrudRepository<FileToken, Long> {
    fun findByToken(token: String): Mono<FileToken>
}
