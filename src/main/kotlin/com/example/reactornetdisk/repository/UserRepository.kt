package com.example.reactornetdisk.repository

import com.example.reactornetdisk.entity.User
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono

interface UserRepository : ReactiveCrudRepository<User, Long> {

    fun findUserByUsername(username: String): Mono<User>
}
