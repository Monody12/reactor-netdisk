package com.example.reactornetdisk.service

import com.example.reactornetdisk.dto.UserDTO
import com.example.reactornetdisk.entity.User
import com.example.reactornetdisk.exception.InvalidPasswordException
import com.example.reactornetdisk.exception.UserNotFoundException
import com.example.reactornetdisk.repository.UserRepository
import com.example.reactornetdisk.repository.UserTokenRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Profile
import org.springframework.test.context.ActiveProfiles
import reactor.test.StepVerifier

@Profile("test")
@SpringBootTest
@ActiveProfiles("test")
class UserServiceTest(
    @Autowired private val userService: UserService,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val userTokenRepository: UserTokenRepository
) {

    @BeforeEach
    fun setUp() {
        userRepository.deleteAll().block()
        userTokenRepository.deleteAll().block()
    }

    @Test
    fun `register new user successfully`() {
        val dto = UserDTO(username = "testuser", password = "password123", email = "test@example.com")

        StepVerifier.create(userService.createUser(dto))
            .assertNext { user ->
                assertNotNull(user.id)
                assertEquals("testuser", user.username)
                assertEquals("password123", user.password)
                assertEquals("test@example.com", user.email)
            }
            .verifyComplete()
    }

    @Test
    fun `register duplicate username throws DuplicateKeyException`() {
        val dto1 = UserDTO(username = "alice", password = "pass1", email = "alice@example.com")
        userService.createUser(dto1).block()

        val dto2 = UserDTO(username = "alice", password = "pass2", email = "alice2@example.com")
        StepVerifier.create(userService.createUser(dto2))
            .expectError()
            .verify()
    }

    @Test
    fun `login with correct password returns token`() {
        val registerDto = UserDTO(username = "loginuser", password = "correctpass", email = null)
        userService.createUser(registerDto).block()

        val loginDto = UserDTO(username = "loginuser", password = "correctpass", email = null)

        StepVerifier.create(userService.login(loginDto))
            .assertNext { userWithToken ->
                assertNotNull(userWithToken.token)
                assertEquals("loginuser", userWithToken.user.username)
                assertTrue(userWithToken.user.password.isEmpty())
            }
            .verifyComplete()
    }

    @Test
    fun `login with wrong password throws InvalidPasswordException`() {
        val registerDto = UserDTO(username = "secureuser", password = "correctpass", email = null)
        userService.createUser(registerDto).block()

        val wrongPassDto = UserDTO(username = "secureuser", password = "wrongpass", email = null)

        StepVerifier.create(userService.login(wrongPassDto))
            .expectError(InvalidPasswordException::class.java)
            .verify()
    }

    @Test
    fun `login with non-existent username throws UserNotFoundException`() {
        val dto = UserDTO(username = "ghost", password = "anypass", email = null)

        StepVerifier.create(userService.login(dto))
            .expectError(UserNotFoundException::class.java)
            .verify()
    }

    @Test
    fun `login generates unique tokens for same user`() {
        val registerDto = UserDTO(username = "multilogin", password = "pass", email = null)
        userService.createUser(registerDto).block()

        val loginDto = UserDTO(username = "multilogin", password = "pass", email = null)
        val token1 = userService.login(loginDto).map { it.token }.block()!!
        val token2 = userService.login(loginDto).map { it.token }.block()!!

        assertNotEquals(token1, token2)
    }

    @Test
    fun `getAllUsers returns all registered users`() {
        userService.createUser(UserDTO(username = "user1", password = "p1", email = null)).block()
        userService.createUser(UserDTO(username = "user2", password = "p2", email = null)).block()

        StepVerifier.create(userService.getAllUsers())
            .assertNext { assertEquals("user1", it.username) }
            .assertNext { assertEquals("user2", it.username) }
            .verifyComplete()
    }

    @Test
    fun `getUserById returns correct user`() {
        val created = userService.createUser(
            UserDTO(username = "findme", password = "secret", email = "findme@example.com")
        ).block()!!

        StepVerifier.create(userService.getUserById(created.id!!))
            .assertNext { user ->
                assertEquals("findme", user.username)
                assertEquals("findme@example.com", user.email)
            }
            .verifyComplete()
    }
}
