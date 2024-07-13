package com.example.reactornetdisk.exception

abstract class TokenException (message: String) : RuntimeException(message)
class TokenNotFoundException(message: String = "Token不存在") : TokenException(message)
