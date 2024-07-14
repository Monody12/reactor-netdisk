package com.example.reactornetdisk.config

import com.example.reactornetdisk.entity.ApiResponse
import com.example.reactornetdisk.entity.UserToken
import com.example.reactornetdisk.exception.TokenException
import com.example.reactornetdisk.exception.TokenNotFoundException
import com.example.reactornetdisk.repository.FileRepository
import com.example.reactornetdisk.repository.FileTokenRepository
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

    @Autowired
    lateinit var fileTokenRepository: FileTokenRepository

    @Autowired
    lateinit var fileRepository: FileRepository

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        // 如果url是登录接口、静态资源，直接放行
        val url = exchange.request.uri.path
        if (url == "/api/user/login" || url.startsWith("/api").not()) {
            return chain.filter(exchange)
        }
        // 如果访问文件资源，则需要校验token
        if (url.startsWith("/api/files/download")) {
            val token: String? = exchange.request.queryParams.getFirst("token")
            val fileId: Long
            try {
                fileId = exchange.request.queryParams.getFirst("fileId")!!.toLong()
            }catch (e : Exception) {
                return returnFileIdInValid(exchange, "访问的文件id不正确")
            }
            // 检查被访问文件是否是公开的，如果是，立即放行
            return  fileRepository.existsFileByIdAndPublicFlagIsTrue(fileId).flatMap {
                if (it) {
                    chain.filter(exchange)
                } else {
                    if (token == null) {
                        return@flatMap returnTokenInValid(exchange, "访问文件资源令牌不存在")
                    }
                    fileTokenRepository.findByToken(token)
                        .switchIfEmpty(Mono.error(TokenNotFoundException("访问文件资源令牌无效")))
                        .flatMap { fileToken ->
                            if (fileToken.expireAt!! < LocalDateTime.now()) {
                                // Token过期
                                returnTokenInValid(exchange, "访问文件资源令牌已过期，请文件所有者重新生成")
                            } else if (fileToken.fileId != fileId) {
                                returnTokenInValid(exchange, "访问文件资源令牌与文件不匹配")
                            } else {
                                chain.filter(exchange)
                            }
                        }
                }
            }
        }

        val token = exchange.request.headers.getFirst("Authorization")

        return if (token != null) {
            userTokenRepository.findByToken(token)
                .switchIfEmpty {
                    Mono.error(TokenNotFoundException("token无效，请重新登录"))
                }
                .flatMap { userToken ->
                    if (userToken.expireAt!! < LocalDateTime.now()) {
                        // Token过期
                        returnUnauthorized(exchange, "token过期，请重新登录")
                    } else {
                        userTokenRepository.updateExpireAtByToken(
                            token = userToken.token!!,
                            expireAt = LocalDateTime.now().plusDays(7)
                        ).flatMap {
                            exchange.attributes["userId"] = userToken.userId
                            chain.filter(exchange)
                        }
                    }
                }

        } else {
            // 未携带token
            returnUnauthorized(exchange, "请登录后操作")
        }
    }

    /**
     * 访问API，返回没有权限
     */
    private fun returnUnauthorized(exchange: ServerWebExchange, msg: String): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.UNAUTHORIZED
        response.headers.contentType = MediaType.APPLICATION_JSON

        val apiResponse = ApiResponse(401, msg, null)
        val responseBody = ObjectMapper().writeValueAsString(apiResponse)

        return response.writeWith(Mono.just(response.bufferFactory().wrap(responseBody.toByteArray())))
    }

    /**
     * 访问文件资源，返回token过期
     */
    private fun returnTokenInValid(exchange: ServerWebExchange, msg: String): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.FORBIDDEN
        response.headers.contentType = MediaType.APPLICATION_JSON

        val apiResponse = ApiResponse(401, msg, null)
        val responseBody = ObjectMapper().writeValueAsString(apiResponse)

        return response.writeWith(Mono.just(response.bufferFactory().wrap(responseBody.toByteArray())))
    }
    /**
     * 访问文件资源，文件id不正确
     */
    private fun returnFileIdInValid(exchange: ServerWebExchange, msg: String): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.NOT_FOUND
        response.headers.contentType = MediaType.APPLICATION_JSON

        val apiResponse = ApiResponse(404, msg, null)
        val responseBody = ObjectMapper().writeValueAsString(apiResponse)

        return response.writeWith(Mono.just(response.bufferFactory().wrap(responseBody.toByteArray())))
    }
}
