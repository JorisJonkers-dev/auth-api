package com.jorisjonkers.personalstack.auth.config

import com.jorisjonkers.personalstack.common.command.Command
import com.jorisjonkers.personalstack.common.command.CommandBus
import com.jorisjonkers.personalstack.common.command.CommandHandler
import org.springframework.aop.support.AopUtils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import kotlin.reflect.KClass
import kotlin.reflect.full.allSupertypes

@Configuration
class CommandBusConfig {
    @Bean
    fun commandBus(handlers: List<CommandHandler<*>>): CommandBus = SpringCommandBus(handlers)
}

class SpringCommandBus(
    handlers: List<CommandHandler<*>>,
) : CommandBus {
    // Each value is a type-erased adapter created once at registration time. Calling
    // CommandHandler<*>.handle via the erased bridge method avoids an unchecked cast —
    // the KClass key guarantees only the matching command type reaches each handler.
    private val handlerMap: Map<KClass<*>, (Command) -> Unit> =
        buildMap {
            for (handler in handlers) {
                val commandType =
                    resolveCommandType(handler)
                        ?: error("Cannot determine command type for ${handler::class.simpleName}")
                put(commandType, erased(handler))
            }
        }

    override fun <T : Command> dispatch(command: T) {
        val adapter =
            handlerMap[command::class]
                ?: error("No handler registered for ${command::class.simpleName}")
        adapter(command)
    }

    // The handler may be wrapped in a CGLIB proxy (Spring AOP) which
    // loses the `CommandHandler<T>` generic type parameter. Unwrap to
    // the original target class and walk *all* supertypes — not just
    // the direct ones — so the lookup works whether the bean is the
    // raw handler, a JDK proxy, or a CGLIB subclass.
    private fun resolveCommandType(handler: CommandHandler<*>): KClass<*>? =
        AopUtils
            .getTargetClass(handler)
            .kotlin
            .allSupertypes
            .firstOrNull { it.classifier == CommandHandler::class }
            ?.arguments
            ?.firstOrNull()
            ?.type
            ?.classifier as? KClass<*>

    companion object {
        // The JVM erases CommandHandler<T>.handle to handle(Command), so we can
        // look up the method by the erased signature and invoke it directly.
        // This avoids a Kotlin UNCHECKED_CAST while staying type-safe: dispatch()
        // routes only the matching KClass to each handler.
        private val handleMethod =
            CommandHandler::class.java.getMethod("handle", Command::class.java)

        private fun erased(handler: CommandHandler<*>): (Command) -> Unit {
            return { command -> handleMethod.invoke(handler, command) }
        }
    }
}
