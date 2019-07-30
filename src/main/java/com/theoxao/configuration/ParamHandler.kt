@file:Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")

package com.theoxao.configuration

import com.theoxao.resolver.method.*
import com.theoxao.resolver.method.`named-value`.CookieValueMethodArgumentResolver
import com.theoxao.resolver.method.`named-value`.PathVariableMethodArgumentResolver
import com.theoxao.resolver.method.`named-value`.RequestHeaderMethodArgumentResolver
import com.theoxao.resolver.method.`named-value`.RequestParamMethodArgumentResolver
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.util.pipeline.PipelineContext
import org.apache.commons.collections4.map.MultiKeyMap
import org.springframework.core.MethodParameter
import org.springframework.core.ParameterNameDiscoverer
import org.springframework.ui.Model
import org.springframework.validation.support.BindingAwareConcurrentModel
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.lang.reflect.Method
import java.lang.reflect.Parameter
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.coroutines.Continuation

/**
 * @author theo
 * @date 2019/4/26
 */

data class Param(val type: Class<*>, var value: Any?, var methodParam: Parameter, val method: Method)
val argumentResolvers = listOf<HandlerMethodArgumentResolver>(
        CallArgumentResolver(),
        RequestParamMapMethodArgumentResolver(),
        RequestBodyArgumentResolver(false),
        CookieValueMethodArgumentResolver(),
        PathVariableMethodArgumentResolver(),
        RequestHeaderMethodArgumentResolver(false),
        RequestParamMethodArgumentResolver(),
        RequestHeaderMethodArgumentResolver(true), //ignore annotation;
//        RequestBodyArgumentResolver(true),
        FinalModelArgumentResolver()
)

val resolverCache: MultiKeyMap<String, HandlerMethodArgumentResolver> = MultiKeyMap()

@KtorExperimentalLocationsAPI
suspend fun PipelineContext<Unit, ApplicationCall>.handlerParam(method: Method, parameterNameDiscoverer: ParameterNameDiscoverer): List<Param> {
    val methodParams = method.parameters.mapIndexed { _, it ->
        Param(it.type, null, it, method)
    }
    val methodSignature = method.signature()
    methodParams.forEachIndexed { index, param ->
        param.value = when (param.methodParam.type) {
            Continuation::class.java -> null
            else -> {
                val methodParameter = MethodParameter(param.method, index)
                methodParameter.initParameterNameDiscovery(parameterNameDiscoverer)
                var result: Any? = null
                val parameterName = methodParameter.parameterName
                val cachedResolver = resolverCache.get(methodSignature, parameterName)
                if (cachedResolver != null) {
                    try {
                        result = cachedResolver.resolverArgument(methodParameter, null, call.request, null)
                    } catch (ignore: Exception) {
                    }
                }
                if (result == null) {
                    for (resolver in argumentResolvers) {
                        if (resolver.supportsParameter(methodParameter)) {
                            try {
                                result = resolver.resolverArgument(methodParameter, null, call.request, null)
                                if (result != null) {
                                    resolverCache.put(methodSignature, parameterName, resolver)
                                    break
                                }
                            } catch (ignore: Exception) {
                            }
                        }
                    }
                }
                result
            }
        }
    }
    return methodParams
}

fun Method.signature() = this.declaringClass.name +
        "#${this.name}" +
        "(${if (this.parameterTypes.isNotEmpty()) this.parameterTypes.map { it.name }.reduce { acc, s -> "$acc,$s" } else ""})"
//        ":${this.returnType.name}"

