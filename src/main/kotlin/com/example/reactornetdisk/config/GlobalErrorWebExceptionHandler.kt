package com.example.reactornetdisk.config

import com.example.reactornetdisk.entity.ApiResponse
import com.example.reactornetdisk.exception.TokenException
import com.example.reactornetdisk.exception.TokenNotFoundException
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
@Order(-2) // 确保它在默认的 ErrorWebExceptionHandler 之前运行
class GlobalErrorWebExceptionHandler : ErrorWebExceptionHandler {
    override fun handle(exchange: ServerWebExchange, ex: Throwable): Mono<Void> {
        val response = exchange.response
//        response.statusCode = HttpStatus.INTERNAL_SERVER_ERROR
        response.headers.contentType = MediaType.APPLICATION_JSON
        val apiResponse : ApiResponse<Any>
         when(ex) {
            is IllegalArgumentException -> {
                apiResponse = ApiResponse(400, ex.message ?: "请求参数错误", null)
                response.statusCode = HttpStatus.BAD_REQUEST
            }
            is TokenNotFoundException -> {
                apiResponse = ApiResponse(401, ex.message ?: "令牌不存在", null)
                response.statusCode = HttpStatus.UNAUTHORIZED
            }
            else -> {
                apiResponse = ApiResponse(500, "服务器错误", ex.localizedMessage)
                response.statusCode = HttpStatus.INTERNAL_SERVER_ERROR
            }
        }
        ex.printStackTrace()
        val responseBody = ObjectMapper().writeValueAsString(apiResponse)
        return response.writeWith(Mono.just(response.bufferFactory().wrap(responseBody.toByteArray())))
    }
}