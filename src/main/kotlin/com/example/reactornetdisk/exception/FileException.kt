package com.example.reactornetdisk.exception

open class FileException(message: String) : RuntimeException(message)
class FileNotFoundInDatabaseException(message: String = "文件不存在于数据库中") : FileException(message)
class FileNotFoundInFileSystemException(message: String = "文件不存在于文件系统中") : FileException(message)
