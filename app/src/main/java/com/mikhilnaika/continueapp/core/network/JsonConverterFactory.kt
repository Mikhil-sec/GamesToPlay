package com.mikhilnaika.continueapp.core.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

/**
 * A small in-house Retrofit [Converter.Factory] for kotlinx.serialization, built on
 * `kotlinx.serialization.serializer(Type)` (a public JVM helper in `kotlinx-serialization-
 * core` itself). Written to replace `com.jakewharton.retrofit:retrofit2-kotlinx-
 * serialization-converter`, whose published class failed to resolve from Kotlin sources in
 * this project's toolchain (visible via `javap`, but "Unresolved reference" from kotlinc)
 * despite resolving correctly in Gradle's dependency graph — root cause not worth chasing
 * further when the replacement is ~20 lines against a public API.
 */
class JsonConverterFactory(
    private val json: Json,
    private val contentType: MediaType,
) : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): Converter<ResponseBody, *> {
        val serializer = json.serializersModule.serializer(type)
        return object : Converter<ResponseBody, Any?> {
            override fun convert(value: ResponseBody): Any? =
                value.use { json.decodeFromString(serializer, it.string()) }
        }
    }

    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<out Annotation>,
        methodAnnotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): Converter<*, RequestBody> {
        val serializer = json.serializersModule.serializer(type)
        return object : Converter<Any, RequestBody> {
            override fun convert(value: Any): RequestBody =
                json.encodeToString(serializer, value).toRequestBody(contentType)
        }
    }

    companion object {
        fun create(json: Json, contentType: MediaType): JsonConverterFactory =
            JsonConverterFactory(json, contentType)
    }
}
