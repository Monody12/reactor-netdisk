package com.example.reactornetdisk.exception

open class UserException(message: String) : RuntimeException(message)
class UserNotFoundException(message: String = "用户不存在") : UserException(message)
class InvalidPasswordException(message: String = "密码错误") : UserException(message)