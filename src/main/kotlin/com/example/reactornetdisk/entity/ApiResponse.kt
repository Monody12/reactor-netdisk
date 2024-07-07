package com.example.reactornetdisk.entity

data class ApiResponse<T>(
    val code: Int,
    val msg: String,
    val data: T?
)