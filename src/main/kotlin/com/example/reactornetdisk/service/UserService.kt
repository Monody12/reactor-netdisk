package com.example.reactornetdisk.service

import com.example.reactornetdisk.dto.UserDTO
import com.example.reactornetdisk.dto.UserWithToken
import com.example.reactornetdisk.entity.User
import com.example.reactornetdisk.entity.UserToken
import com.example.reactornetdisk.exception.InvalidPasswordException
import com.example.reactornetdisk.exception.UserNotFoundException
import com.example.reactornetdisk.repository.UserRepository
import com.example.reactornetdisk.repository.UserTokenRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userTokenRepository: UserTokenRepository,
) {

    fun createUser(dto: UserDTO): Mono<User> {
        val user = User(
            username = dto.username,
            password = dto.password,
            email = dto.email
        )
        return userRepository.save(user)
    }

    fun getAllUsers(): Flux<User> = userRepository.findAll()

    fun getUserById(id: Int): Mono<User> = userRepository.findById(id)

    fun updateUser(id: Int, user: User): Mono<User> {
        return userRepository.findById(id)
            .flatMap { existingUser ->
                userRepository.save(user.copy(id = existingUser.id))
            }
    }

    fun deleteUser(id: Int): Mono<Void> = userRepository.deleteById(id)
    fun login(userDTO: UserDTO): Mono<UserWithToken> {
        return userRepository.findUserByUsername(userDTO.username)
            .switchIfEmpty(Mono.error(UserNotFoundException()))
            .flatMap { user ->
                if (user.password == userDTO.password) {
                    val token = UUID.randomUUID().toString().replace("-", "")
                    val userTokenEntity = UserToken(userId = user.id, token = token)
                    user.password = ""
                    userTokenRepository.save(userTokenEntity)
                        .map {
                            UserWithToken(
                                user = user,
                                token = token
                            )
                        }
                } else {
                    Mono.error(InvalidPasswordException())
                }
            }
    }
}