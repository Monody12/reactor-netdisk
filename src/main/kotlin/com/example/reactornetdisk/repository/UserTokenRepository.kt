package com.example.reactornetdisk.repository

import com.example.reactornetdisk.entity.UserToken
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono
import java.time.LocalDateTime

interface UserTokenRepository : ReactiveCrudRepository<UserToken, Long> {
    fun findByToken(token: String): Mono<UserToken>
    fun updateUserTokenByToken(userToken: UserToken): Mono<Int>
}
