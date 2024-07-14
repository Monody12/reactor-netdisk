package com.example.reactornetdisk.controller

import com.example.reactornetdisk.dto.UploadForm
import com.example.reactornetdisk.entity.ApiResponse
import com.example.reactornetdisk.entity.FileToken
import com.example.reactornetdisk.exception.FileForbiddenException
import com.example.reactornetdisk.exception.FileNotFoundInDatabaseException
import com.example.reactornetdisk.exception.FileNotFoundInFileSystemException
import com.example.reactornetdisk.service.FileService
import org.apache.tika.Tika
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.core.io.support.ResourceRegion
import org.springframework.http.*
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.Stack


@RequestMapping("/api")
@RestController
class FileController(
    private val fileService: FileService,
    @Value("\${file.upload.path}") private val uploadPath: String
) {

    @PostMapping("/upload")
    fun uploadFiles(
        @RequestPart("files") filePartFlux: Flux<FilePart>,
        @ModelAttribute uploadForm : UploadForm,
        exchange: ServerWebExchange
    ): Mono<ApiResponse<List<com.example.reactornetdisk.entity.File>>> {
        return fileService.saveFiles(
            filePartFlux = filePartFlux,
            folderId = uploadForm.folderId,
            publicFlag = uploadForm.publicFlag,
            userId = exchange.attributes["userId"] as Int
        ).collectList()
            .map {
            ApiResponse(200, "上传完成", it)
        }.doOnSuccess { response ->
            // 在这里进行成功后的操作，但不修改响应
            println("文件上传成功: $response")
        }.doOnError { error ->
            // 在这里处理错误，但不修改响应
            println("文件上传失败: ${error.message}")
        }
    }

    /**
     * 申请文件访问token
     */
    @GetMapping("/files/token")
    fun applyFileToken(
        @RequestParam fileId: Long,
        exchange: ServerWebExchange
    ): Mono<ApiResponse<FileToken>> {
        val userId = exchange.attributes["userId"] as Int
        return fileService.applyFileToken(userId, fileId)
            .switchIfEmpty {
                Mono.error(FileNotFoundInDatabaseException())
            }
            .map { ApiResponse(200, "文件访问令牌生成成功", it) }
    }

//    @PostMapping("/upload/test")
//    fun uploadFilesTest(
//        @RequestPart("files") filePartFlux: Flux<FilePart>
//    ):Flux<Any>{
//        return filePartFlux.flatMap { filePart ->
//            filePart.transferTo(Paths.get(uploadPath,filePart.filename()))
//                .then(Mono.just(filePart.filename()))
//        }
//    }

    @GetMapping("/files/download")
    fun downloadFile(
        @RequestParam fileId: Long,
        @RequestParam(defaultValue = "false") preview: Boolean,
        @RequestHeader(value = "Range", required = false) rangeHeader: String?,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<out Resource>> {
//        val requestUserId = exchange.attributes["userId"] as Int
//        val requestFileId = exchange.attributes["fileId"] as Long
        return fileService.getFileById(fileId)
            .switchIfEmpty(Mono.error(FileNotFoundInDatabaseException()))
            .flatMap { dataFile ->
                val fileUploadName = dataFile.name
                val fileAbsolutePath = Paths.get(uploadPath, dataFile.pathName.replace("/", File.separator))
                val storageFile = File(fileAbsolutePath.toString())
                if (!storageFile.exists()) {
                    Mono.error(FileNotFoundInFileSystemException())
                } else {
                    val resource = FileSystemResource(storageFile)
                    handleFileDownload(resource, fileUploadName, preview.not(), rangeHeader)
                        .onErrorResume { e ->
                            when (e) {
                                is IOException -> {
                                    println("IO error during file download: ${e.message}")
                                    Mono.empty()
                                }

                                else -> Mono.error(e)
                            }
                        }
                }
            }
    }

    private fun handleFileDownload(
        resource: FileSystemResource,
        fileName: String,
        isDownload: Boolean,
        rangeHeader: String?
    ): Mono<ResponseEntity<out Resource>> {
        return Mono.defer {
            val fileLength = resource.contentLength()
            val contentType: String = Tika().detect(fileName)

            val headers = HttpHeaders()
            headers.contentType = MediaType.parseMediaType(contentType)
            headers.add(HttpHeaders.ACCEPT_RANGES, "bytes")

            val encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString())
                .replace("+", "%20")

            val contentDisposition = if (isDownload) {
                "attachment; filename=\"$encodedFileName\"; filename*=UTF-8''$encodedFileName"
            } else {
                "inline; filename=\"$encodedFileName\"; filename*=UTF-8''$encodedFileName"
            }
            headers.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)

            if (rangeHeader != null) {
                val ranges = HttpRange.parseRanges(rangeHeader)
                if (ranges.isEmpty()) {
                    ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                        .headers(headers)
                        .build<Resource>()
                        .toMono()
                } else {
                    val range = ranges[0]
                    val start = range.getRangeStart(fileLength)
                    val end = range.getRangeEnd(fileLength)
                    val contentLength = end - start + 1
                    headers.contentLength = contentLength
                    headers.add(HttpHeaders.CONTENT_RANGE, "bytes $start-$end/$fileLength")
                    ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                        .headers(headers)
                        .body(resource)
                        .toMono()
                }
            } else {
                headers.contentLength = fileLength
                ResponseEntity.ok()
                    .headers(headers)
                    .body(resource)
                    .toMono()
            }
        }
    }

}
