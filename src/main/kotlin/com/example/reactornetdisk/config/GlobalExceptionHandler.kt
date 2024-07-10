package com.example.reactornetdisk.config

import com.example.reactornetdisk.entity.ApiResponse
import com.example.reactornetdisk.exception.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.reactive.resource.NoResourceFoundException
import org.springframework.web.server.ServerWebInputException
import reactor.core.publisher.Mono

// 全局异常处理器
@ControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(UserException::class)
    fun handleUserException(ex: UserException): Mono<ResponseEntity<ApiResponse<out String>>> {
        ex.printStackTrace()
        val response = when (ex) {
            is UserNotFoundException -> ApiResponse(404, ex.message ?: "用户不存在", null)
            is InvalidPasswordException -> ApiResponse(401, ex.message ?: "密码错误", null)
            else -> ApiResponse(500, "未定义用户模块错误", ex.localizedMessage)
        }
        return Mono.just(ResponseEntity(response, HttpStatus.OK))
    }

    @ExceptionHandler(FileException::class)
    fun handleFileException(ex: FileException): Mono<ResponseEntity<ApiResponse<out String>>> {
        ex.printStackTrace()
        val response = when (ex) {
            is FileNotFoundInDatabaseException -> ApiResponse(404, ex.message ?: "", null)
            is FileNotFoundInFileSystemException -> ApiResponse(500, ex.message ?: "", null)
            is FolderNotFoundException -> ApiResponse(404, ex.message ?: "", null)
            is FileForbiddenException -> ApiResponse(403, ex.message ?: "", null)
            else -> ApiResponse(500, "未定义文件模块错误", ex.localizedMessage)
        }
        return Mono.just(ResponseEntity(response, HttpStatus.OK))
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFoundException(ex: NoResourceFoundException): Mono<ResponseEntity<ApiResponse<String>>> {
        ex.printStackTrace()
        val response = ApiResponse(404, "资源未找到", ex.localizedMessage)
        return Mono.just(ResponseEntity(response, HttpStatus.OK))
    }

    @ExceptionHandler(ServerWebInputException::class)
    fun handleServerWebInputException(ex: ServerWebInputException): Mono<ResponseEntity<ApiResponse<String>>> {
        ex.printStackTrace()
        val response = ApiResponse(400, "请求参数错误", ex.localizedMessage)
        return Mono.just(ResponseEntity(response, HttpStatus.OK))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): Mono<ResponseEntity<ApiResponse<String>>> {
        ex.printStackTrace()
        val response = ApiResponse(500, "服务器错误", ex.localizedMessage)
        return Mono.just(ResponseEntity(response, HttpStatus.OK))
    }
}