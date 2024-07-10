package com.example.reactornetdisk.repository

import com.example.reactornetdisk.entity.UserToken
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono
import java.time.LocalDateTime

interface UserTokenRepository : ReactiveCrudRepository<UserToken, Long> {
    fun findByToken(token: String): Mono<UserToken>
    @Modifying
    @Query("UPDATE user_token SET expire_at = :expireAt WHERE token = :token")
    fun updateExpireAtByToken(expireAt: LocalDateTime, token: String): Mono<Int>
}
