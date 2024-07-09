package com.example.reactornetdisk.controller

import com.example.reactornetdisk.entity.ApiResponse
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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Paths


@RequestMapping("/api")
@RestController
class FileController(
    private val fileService: FileService,
    @Value("\${file.upload.path}") private val uploadPath: String
) {

    @PostMapping("/upload")
    fun uploadFiles(
        @RequestPart("files") filePartFlux: Flux<FilePart>,
        @RequestParam(required = false) folderId: Long?
    ): Mono<ApiResponse<String>> {
        return fileService.saveFiles(
            filePartFlux = filePartFlux,
            folderId = folderId,
            userId = 1
        ).map {
            ApiResponse(200, "上传完成", it)
        }
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

    @GetMapping("/files/{id}/download")
    fun downloadFile(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "false") preview: Boolean,
        @RequestHeader(value = "Range", required = false) rangeHeader: String?
    ): Mono<ResponseEntity<out Resource>> {
        return fileService.getFileById(id)
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