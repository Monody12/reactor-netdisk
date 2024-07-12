package com.example.reactornetdisk

import com.example.reactornetdisk.repository.FileRepository
import com.example.reactornetdisk.service.FolderService
import com.example.reactornetdisk.service.UserService
import com.example.reactornetdisk.util.UploadUtil
import org.apache.tika.Tika
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import reactor.test.StepVerifier
import java.io.File
import java.nio.file.Path
import java.time.LocalDateTime

@SpringBootTest
class ReactorNetdiskApplicationTests {

    @Test
    fun contextLoads() {
    }

    @Autowired
    private lateinit var folderService: FolderService

    @Autowired
    lateinit var userService: UserService

    @Autowired
    lateinit var fileRepository: FileRepository

    @Value("\${file.upload.path}")
    lateinit var uploadPath: String

    @Test
    fun selectUser() {
//        StepVerifier.create(userService.getAllUsers())
//            .thenConsumeWhile { user ->
//                println(user)
//                true
//            }
//            .verifyComplete()
        val users = userService.getAllUsers().collectList().block()
        users?.forEach { user -> println(user) }
    }

    @Test
    fun fileUploadTest() {
    }

    @Test
    fun getMimeTypeTest() {
        val fileList = arrayOf(
            "a.txt",
            "b.pdf",
            "c.jpg",
            "d.mp4",
            "e.mp3",
            "f.doc",
            "g.ppt",
            "h.xls",
            "i.zip",
            "j.rar",
            "k.exe",
            "l.png",
            "sort.java",
            "sort.kt",
            "sort.py",
            "sort.js",
            "index.html",
            "hello.css"
        )
        fileList.forEach {
            val mimeType = Tika().detect(it)
            println("$it: $mimeType")
        }
    }

    @Test
    fun judgeStringEqualsLong() {
        val fileId : String? = null
        val dbFileId : Long = 123
        if (fileId.equals(dbFileId.toString())) {
            println(true)
        } else {
            println(false)
        }
    }

}
