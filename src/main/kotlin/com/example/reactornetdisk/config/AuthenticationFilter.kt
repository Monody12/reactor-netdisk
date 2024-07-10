package com.example.reactornetdisk.config

import com.example.reactornetdisk.entity.ApiResponse
import com.example.reactornetdisk.entity.UserToken
import com.example.reactornetdisk.repository.UserTokenRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
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
        // 如果url是登录接口，直接放行
        val url = exchange.request.uri.path
        if (url == "/api/user/login" || url.startsWith("/api").not()) {
            return chain.filter(exchange)
        }

        val token = exchange.request.headers.getFirst("Authorization")

        return if (token != null) {
            userTokenRepository.findByToken(token)
                .flatMap { userToken ->
                    if (userToken.expireAt!! < LocalDateTime.now()) {
                        // Token过期
                        returnUnauthorized(exchange, "token过期，请重新登录")
                    } else {
                        userTokenRepository.updateExpireAtByToken(
                            token = userToken.token!!,
                            expireAt = LocalDateTime.now().plusDays(7)
                        )
                            .flatMap {
                                exchange.attributes["userId"] = userToken.userId
                                chain.filter(exchange)
                            }
                    }
                }
                // 忽略这个警告，代码可以正常执行
                .switchIfEmpty(Mono.defer { returnUnauthorized(exchange,"token无效，请重新登录") })
        } else {
            // 未携带token
            returnUnauthorized(exchange, "请登录后操作")
        }
    }

    private fun returnUnauthorized(exchange: ServerWebExchange, msg: String): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.UNAUTHORIZED
        response.headers.contentType = MediaType.APPLICATION_JSON

        val apiResponse = ApiResponse(401, msg, null)
        val responseBody = ObjectMapper().writeValueAsString(apiResponse)

        return response.writeWith(Mono.just(response.bufferFactory().wrap(responseBody.toByteArray())))
    }
}
