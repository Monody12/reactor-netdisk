package com.example.reactornetdisk.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

@Configuration
class CorsConfig {

    @Bean
    @Order(value = Ordered.HIGHEST_PRECEDENCE + 1)
    fun corsWebFilter(): CorsWebFilter {
        val config = CorsConfiguration()
        config.addAllowedOrigin("https://www.dluserver.cn:8080")
        config.addAllowedOrigin("https://www.dluserver.cn:8443")
        config.addAllowedOrigin("http://localhost:5173")
        config.addAllowedOrigin("http://localhost:5174")
        config.addAllowedMethod("*") // 允许所有 HTTP 方法
        config.addAllowedHeader("*") // 允许所有 Header
        config.allowCredentials = true // 是否允许发送 Cookie

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)

        return CorsWebFilter(source)
    }

}