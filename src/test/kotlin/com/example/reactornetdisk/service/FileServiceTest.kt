package com.example.reactornetdisk.service

import com.example.reactornetdisk.entity.File
import com.example.reactornetdisk.entity.Folder
import com.example.reactornetdisk.repository.FileRepository
import com.example.reactornetdisk.repository.FolderRepository
import com.example.reactornetdisk.repository.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferFactory
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.codec.multipart.FilePart
import org.springframework.test.context.ActiveProfiles
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.nio.ByteBuffer
import java.nio.file.Path
import java.nio.file.Paths

@SpringBootTest
@ActiveProfiles("test")
class FileServiceTest(
    @Autowired private val fileService: FileService,
    @Autowired private val folderRepository: FolderRepository,
    @Autowired private val fileRepository: FileRepository,
    @Autowired private val userRepository: UserRepository
) {

    @Value("\${file.upload.path}")
    lateinit var uploadPath: String

    private val dataBufferFactory: DataBufferFactory = DefaultDataBufferFactory.sharedInstance

    @BeforeEach
    fun setUp() {
        fileRepository.deleteAll().block()
        folderRepository.deleteAll().block()
        userRepository.deleteAll().block()
    }

    @Test
    fun `upload files with folder structure creates nested folders`() {
        val userId = 1
        val parentId: Long? = null
        val publicFlag = false

        // 模拟上传文件 with relative paths
        // photos/vacation/beach.jpg
        // photos/vacation/sunset.jpg
        // photos/city/night.jpg
        val filesWithPath = listOf(
            createMockFilePart("photos/vacation/beach.jpg"),
            createMockFilePart("photos/vacation/sunset.jpg"),
            createMockFilePart("photos/city/night.jpg")
        )

        StepVerifier.create(
            fileService.saveFilesWithFolderStructure(
                filePartsWithPath = Flux.fromIterable(filesWithPath),
                parentId = parentId,
                publicFlag = publicFlag,
                userId = userId
            )
        )
            .assertNext { file ->
                assertEquals("beach.jpg", file.name)
                assertEquals(userId, file.userId)
            }
            .assertNext { file ->
                assertEquals("sunset.jpg", file.name)
                assertEquals(userId, file.userId)
            }
            .assertNext { file ->
                assertEquals("night.jpg", file.name)
                assertEquals(userId, file.userId)
            }
            .verifyComplete()

        // 验证文件夹是否被创建
        StepVerifier.create(folderRepository.findByUserIdAndParentId(userId, null))
            .assertNext { folder ->
                assertEquals("photos", folder.name)
                assertNull(folder.parentId)
            }
            .verifyComplete()
    }

    @Test
    fun `upload files to existing parent folder`() {
        val userId = 1
        val publicFlag = false

        // 首先创建一个父文件夹
        val parentFolder = folderRepository.save(
            Folder(
                userId = userId,
                parentId = null,
                description = null,
                publicFlag = false,
                name = "ExistingFolder"
            )
        ).block()!!

        // 模拟上传文件到 ExistingFolder/subfolder/file.txt
        val filesWithPath = listOf(
            createMockFilePart("subfolder/file1.txt"),
            createMockFilePart("subfolder/file2.txt")
        )

        StepVerifier.create(
            fileService.saveFilesWithFolderStructure(
                filePartsWithPath = Flux.fromIterable(filesWithPath),
                parentId = parentFolder.id,
                publicFlag = publicFlag,
                userId = userId
            )
            .collectList()
        )
            .assertNext { files ->
                assertEquals(2, files.size)
                assertTrue(files.any { it.name == "file1.txt" })
                assertTrue(files.any { it.name == "file2.txt" })
                assertTrue(files.all { it.userId == userId })
            }
            .verifyComplete()

        // 验证 ExistingFolder 下的子文件夹 subfolder 是否被创建
        StepVerifier.create(folderRepository.findByUserIdAndParentId(userId, parentFolder.id))
            .assertNext { folder ->
                assertEquals("subfolder", folder.name)
                assertEquals(parentFolder.id, folder.parentId)
            }
            .verifyComplete()
    }

    @Test
    fun `upload files without path goes to parent folder`() {
        val userId = 1
        val publicFlag = false

        // 创建父文件夹
        val parentFolder = folderRepository.save(
            Folder(
                userId = userId,
                parentId = null,
                description = null,
                publicFlag = false,
                name = "TargetFolder"
            )
        ).block()!!

        // 模拟上传文件，没有路径信息（只有文件名）
        val filesWithPath = listOf(
            createMockFilePart("single_file.txt")
        )

        StepVerifier.create(
            fileService.saveFilesWithFolderStructure(
                filePartsWithPath = Flux.fromIterable(filesWithPath),
                parentId = parentFolder.id,
                publicFlag = publicFlag,
                userId = userId
            )
        )
            .assertNext { file ->
                assertEquals("single_file.txt", file.name)
                assertEquals(parentFolder.id, file.folderId)
            }
            .verifyComplete()
    }

    @Test
    fun `upload files with deep nested structure`() {
        val userId = 1
        val parentId: Long? = null
        val publicFlag = false

        // 模拟深层嵌套结构
        // a/b/c/d/deep_file.txt
        val filesWithPath = listOf(
            createMockFilePart("a/b/c/d/deep_file.txt")
        )

        StepVerifier.create(
            fileService.saveFilesWithFolderStructure(
                filePartsWithPath = Flux.fromIterable(filesWithPath),
                parentId = parentId,
                publicFlag = publicFlag,
                userId = userId
            )
        )
            .assertNext { file ->
                assertEquals("deep_file.txt", file.name)
                assertEquals(userId, file.userId)
            }
            .verifyComplete()

        // 验证所有层级的文件夹都被创建
        StepVerifier.create(folderRepository.findByUserIdAndParentId(userId, null))
            .assertNext { folder ->
                assertEquals("a", folder.name)
            }
            .verifyComplete()
    }

    /**
     * 创建模拟的 FilePart
     */
    private fun createMockFilePart(relativePath: String): Pair<FilePart, String> {
        val content = ByteBuffer.wrap("test content".toByteArray())
        val dataBuffer = dataBufferFactory.wrap(content)

        val mockFilePart = object : FilePart {
            override fun name(): String = "files"  // Part name

            override fun filename(): String = relativePath  // 返回完整路径作为filename

            override fun headers(): org.springframework.http.HttpHeaders = org.springframework.http.HttpHeaders()

            override fun content(): Flux<DataBuffer> = Flux.just(dataBuffer)

            override fun transferTo(dest: Path): Mono<Void> {
                return Mono.fromRunnable {
                    dest.parent.toFile().mkdirs()
                    dest.toFile().writeText("test content")
                }
            }
        }

        return Pair(mockFilePart, relativePath)
    }
}
