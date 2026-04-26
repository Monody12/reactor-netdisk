package com.example.reactornetdisk.service

import com.example.reactornetdisk.entity.Folder
import com.example.reactornetdisk.exception.FolderNotFoundException
import com.example.reactornetdisk.repository.FileRepository
import com.example.reactornetdisk.repository.FolderRepository
import com.example.reactornetdisk.repository.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import reactor.test.StepVerifier

@SpringBootTest
@ActiveProfiles("test")
class FolderServiceTest(
    @Autowired private val folderService: FolderService,
    @Autowired private val folderRepository: FolderRepository,
    @Autowired private val fileRepository: FileRepository,
    @Autowired private val userRepository: UserRepository
) {

    @BeforeEach
    fun setUp() {
        fileRepository.deleteAll().block()
        folderRepository.deleteAll().block()
        userRepository.deleteAll().block()
    }

    @Test
    fun `create folder at root level`() {
        val folder = folderService.createFolder(userId = 1, parentId = null, folderName = "MyFolder").block()!!

        assertNotNull(folder.id)
        assertEquals("MyFolder", folder.name)
        assertEquals(1, folder.userId)
        assertNull(folder.parentId)
        assertFalse(folder.publicFlag)
    }

    @Test
    fun `create nested folder`() {
        val parent = folderService.createFolder(1, null, "Parent").block()!!
        val child = folderService.createFolder(1, parent.id, "Child").block()!!

        assertEquals(parent.id, child.parentId)
        assertEquals("Child", child.name)
    }

    @Test
    fun `get folder id from path - single level`() {
        folderService.createFolder(1, null, "RootFolder").block()!!

        StepVerifier.create(folderService.getFolderIdFromPath(1, "/RootFolder"))
            .assertNext { assertNotNull(it) }
            .verifyComplete()
    }

    @Test
    fun `get folder id from path - multi level`() {
        val p1 = folderService.createFolder(1, null, "Level1").block()!!
        folderService.createFolder(1, p1.id, "Level2").block()!!

        StepVerifier.create(folderService.getFolderIdFromPath(1, "/Level1/Level2"))
            .assertNext { assertNotNull(it) }
            .verifyComplete()
    }

    @Test
    fun `get folder id from path - non-existent folder throws error`() {
        StepVerifier.create(folderService.getFolderIdFromPath(1, "/NonExistent"))
            .expectError(FolderNotFoundException::class.java)
            .verify()
    }

    @Test
    fun `get files and folders - returns both types`() {
        val folder = folderService.createFolder(1, null, "MyFolder").block()!!

        StepVerifier.create(folderService.getFilesAndFolders(1, null, false))
            .assertNext { f -> assertTrue(f.isFolder) }
            .verifyComplete()
    }

    @Test
    fun `delete file and folder returns count message`() {
        val folder = folderService.createFolder(1, null, "ToDelete").block()!!

        StepVerifier.create(folderService.deleteFileAndFolder(1, null, listOf(folder.id!!)))
            .assertNext { msg ->
                assertTrue(msg.contains("删除"))
            }
            .verifyComplete()
    }

    @Test
    fun `delete file and folder with empty lists`() {
        StepVerifier.create(folderService.deleteFileAndFolder(1, null, null))
            .assertNext { msg ->
                assertEquals("共删除0个文件，0个文件夹", msg)
            }
            .verifyComplete()
    }
}
