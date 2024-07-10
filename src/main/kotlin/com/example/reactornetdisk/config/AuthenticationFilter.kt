package com.example.reactornetdisk.config

import com.example.reactornetdisk.entity.UserToken
import com.example.reactornetdisk.repository.UserTokenRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import java.time.LocalDateTime

@Component
class AuthenticationFilter : WebFilter {

    @Autowired
    lateinit var userTokenRepository: UserTokenRepository

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val token = exchange.request.headers.getFirst("Authorization")

        return if (token != null) {
            userTokenRepository.findByToken(token)
                .flatMap { userToken ->
                    userToken.expireAt = LocalDateTime.now().plusDays(7)
                    userTokenRepository.updateUserTokenByToken(userToken).flatMap {
                        exchange.attributes["userId"] = userToken.userId
                        chain.filter(exchange)
                    }
                }
                .switchIfEmpty {
                    exchange.response.statusCode = HttpStatus.UNAUTHORIZED
                    exchange.response.setComplete()
                }

        } else {
            exchange.response.statusCode = HttpStatus.UNAUTHORIZED
            exchange.response.setComplete()
        }
    }

}