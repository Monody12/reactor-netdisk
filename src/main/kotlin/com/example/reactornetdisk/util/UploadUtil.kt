package com.example.reactornetdisk.util

import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

object UploadUtil {
    /**
     * 获取日期路径，用于拼接文件上传路径
     * 例如 /2021/08/01
     * @param fileSeparator 文件分隔符
     */
    fun getDatePath(
        fileSeparator : String, localDate: LocalDate = LocalDate.now()
    ): String {
        val (year, month, day) = arrayOf(localDate.year, localDate.monthValue, localDate.dayOfMonth)
        return fileSeparator + year + fileSeparator + month + fileSeparator + day
    }
}