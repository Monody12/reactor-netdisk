package com.example.reactornetdisk.service

import com.example.reactornetdisk.dto.UserDTO
import com.example.reactornetdisk.entity.User
import com.example.reactornetdisk.exception.InvalidPasswordException
import com.example.reactornetdisk.exception.UserNotFoundException
import com.example.reactornetdisk.repository.UserRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class UserService(private val userRepository: UserRepository) {

    fun createUser(dto: UserDTO): Mono<User> {
        val user = User(
            username = dto.username,
            password = dto.password,
            email = dto.email
        )
        return userRepository.save(user)
    }

    fun getAllUsers(): Flux<User> = userRepository.findAll()

    fun getUserById(id: Long): Mono<User> = userRepository.findById(id)

    fun updateUser(id: Long, user: User): Mono<User> {
        return userRepository.findById(id)
            .flatMap { existingUser ->
                userRepository.save(user.copy(id = existingUser.id))
            }
    }

    fun deleteUser(id: Long): Mono<Void> = userRepository.deleteById(id)
    fun login(userDTO: UserDTO): Mono<User> {
        return userRepository.findUserByUsername(userDTO.username)
            .switchIfEmpty(Mono.error(UserNotFoundException()))
            .flatMap { user ->
                if (user.password == userDTO.password) {
                    Mono.just(user)
                } else {
                    Mono.error(InvalidPasswordException())
                }
            }
    }
}