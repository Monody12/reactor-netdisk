package com.example.reactornetdisk.config

import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.format.FormatterRegistry
import org.springframework.stereotype.Component
import org.springframework.web.reactive.config.WebFluxConfigurer

@Component
class StringToNullableLongConverter : Converter<String, Long?> {
    override fun convert(source: String): Long? {
        return when {
            source.isBlank() -> null
            source.equals("null", ignoreCase = true) -> null
            else -> source.toLongOrNull()
        }
    }
}

@Configuration
class WebFluxConfig : WebFluxConfigurer {
    override fun addFormatters(registry: FormatterRegistry) {
        registry.addConverter(StringToNullableLongConverter())
    }

}