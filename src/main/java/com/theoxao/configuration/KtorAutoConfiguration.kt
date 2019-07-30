package com.theoxao.configuration

import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.features.PartialContent
import io.ktor.gson.gson
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Locations
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.EngineAPI
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.sessions.Sessions
import io.ktor.util.KtorExperimentalAPI
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.RequestMethod
import java.lang.reflect.Method
import javax.annotation.Resource
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.reflect.jvm.kotlinFunction


/**
 * @author theo
 * @date 2019/4/16
 */
@Configuration
@EnableConfigurationProperties(KtorProperties::class)
open class KtorAutoConfiguration {

    @Resource
    private lateinit var properties: KtorProperties

    @Resource
    private lateinit var context: ApplicationContext

    @KtorExperimentalLocationsAPI
    @EngineAPI
    @KtorExperimentalAPI
    @Bean
    open fun applicationEngine(context: ApplicationContext): ApplicationEngine {
        val engineFactory = Netty

        return embeddedServer(engineFactory, properties.port, properties.host) {
            install(ContentNegotiation) {
                gson {

                }
            }
            install(Locations)
            install(Sessions)
            install(PartialContent)
            install(CORS) {
                anyHost()
            }
        }.start(wait = false)
    }

}

data class RouteDefinition(val method: Method, val bean: Any, var methods: Array<RequestMethod>, val uri: List<String>)

/**
 * invoke method
 * if its suspend, add continuation
 */
suspend fun Method.invokeSuspend(obj: Any, args: Array<*>): Any? =
        if (this.kotlinFunction != null && this.kotlinFunction!!.isSuspend) {
            suspendCoroutineUninterceptedOrReturn<Any> {
                //add continuation at the end of args
                val list = arrayListOf(*args)
                list[list.size - 1] = it
                invoke(obj, *list.toArray())
            }
        } else {
            invoke(obj, *args)
        }

