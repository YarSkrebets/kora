package ru.tinkoff.grpc.client.ksp

import org.assertj.core.api.Assertions
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import ru.tinkoff.kora.kora.app.ksp.KoraAppProcessorProvider
import ru.tinkoff.kora.ksp.common.AbstractSymbolProcessorTest
import ru.tinkoff.kora.ksp.common.GraphUtil.toGraph
import java.util.*

class GrpcClientExtensionTest : AbstractSymbolProcessorTest() {
    protected fun compile(@Language("kotlin") vararg sources: String) {
        val patchedSources = Arrays.copyOf(sources, sources.size + 1)
        patchedSources[sources.size] = """
            @ru.tinkoff.kora.common.Module
            interface ConfigModule : ru.tinkoff.grpc.client.GrpcNettyClientModule {
              fun config(): com.typesafe.config.Config {
                return com.typesafe.config.ConfigFactory.parseMap(mapOf(
                  "testPath" to mapOf(
                    "url" to "http://localhost:8080",
                    "timeout" to "20s"
                  )
                ));
              }
            }
            
            """.trimIndent()
        super.compile(listOf(KoraAppProcessorProvider(), GrpcClientStubForSymbolProcessorProvider()), *patchedSources)
        compileResult.assertSuccess()
        loadClass("TestAppGraph").toGraph().use { g ->
            /*
            1. root config
            2. parsed config
            3. netty event loop group
            4. netty boss event loop group  (will be removed after @Root fix)
            5. channel factory
            6. telemetry factory
            7. channel lifecycle
            8. service descriptor
            9. the stub
            10. test root
            */
            Assertions.assertThat(g.draw.size()).isEqualTo(10)
        }
    }


    @Test
    fun testBlockingStub() {
        compile("""
            @KoraApp
            interface TestApp {
              @Tag(ru.tinkoff.kora.grpc.server.events.EventsGrpc::class)
              fun config(config: com.typesafe.config.Config): ru.tinkoff.grpc.client.config.GrpcClientConfig {
                return ru.tinkoff.grpc.client.config.GrpcClientConfig.fromConfig(config, "testPath")
              }
            
              @Root
              fun test(stub: ru.tinkoff.kora.grpc.server.events.EventsGrpc.EventsBlockingStub): String {
                return ""
              }
            }
            
            """.trimIndent())
    }

    @Test
    fun testFutureStub() {
        compile("""
            @KoraApp
            interface TestApp {
              @Tag(ru.tinkoff.kora.grpc.server.events.EventsGrpc::class)
              fun config(config: com.typesafe.config.Config): ru.tinkoff.grpc.client.config.GrpcClientConfig {
                return ru.tinkoff.grpc.client.config.GrpcClientConfig.fromConfig(config, "testPath")
              }
            
              @Root
              fun test(stub: ru.tinkoff.kora.grpc.server.events.EventsGrpc.EventsFutureStub): String {
                return ""
              }
            }
            
            """.trimIndent())
    }

    @Test
    fun testAsyncStub() {
        compile("""
            @KoraApp
            interface TestApp {
              @Tag(ru.tinkoff.kora.grpc.server.events.EventsGrpc::class)
              fun config(config: com.typesafe.config.Config): ru.tinkoff.grpc.client.config.GrpcClientConfig {
                return ru.tinkoff.grpc.client.config.GrpcClientConfig.fromConfig(config, "testPath")
              }
            
              @Root
              fun test(stub: ru.tinkoff.kora.grpc.server.events.EventsGrpc.EventsStub): String {
                return ""
              }
            }
            
            """.trimIndent())
    }

    @Test
    fun testCoroutineStubStub() {
        compile("""
            @KoraApp
            interface TestApp {
              @Tag(ru.tinkoff.kora.grpc.server.events.EventsGrpc::class)
              fun config(config: com.typesafe.config.Config): ru.tinkoff.grpc.client.config.GrpcClientConfig {
                return ru.tinkoff.grpc.client.config.GrpcClientConfig.fromConfig(config, "testPath")
              }
            
              @Root
              fun test(stub: EventsGrpcKt.EventsCoroutineStub): String {
                return ""
              }
            }
            
            """.trimIndent(),
            """
                import ru.tinkoff.kora.grpc.server.events.*
                import io.grpc.CallOptions
                import io.grpc.CallOptions.DEFAULT
                import io.grpc.Channel
                import io.grpc.Metadata
                import io.grpc.MethodDescriptor
                import io.grpc.ServerServiceDefinition
                import io.grpc.ServerServiceDefinition.builder
                import io.grpc.ServiceDescriptor
                import io.grpc.Status.UNIMPLEMENTED
                import io.grpc.StatusException
                import io.grpc.kotlin.AbstractCoroutineServerImpl
                import io.grpc.kotlin.AbstractCoroutineStub
                import io.grpc.kotlin.ClientCalls.unaryRpc
                import io.grpc.kotlin.ServerCalls.unaryServerMethodDefinition
                import io.grpc.kotlin.StubFor
                import kotlin.String
                import kotlin.coroutines.CoroutineContext
                import kotlin.coroutines.EmptyCoroutineContext
                import kotlin.jvm.JvmOverloads
                import kotlin.jvm.JvmStatic
                import ru.tinkoff.kora.grpc.server.events.EventsGrpc.getServiceDescriptor

                public object EventsGrpcKt {
                  public const val SERVICE_NAME: String = EventsGrpc.SERVICE_NAME

                  @JvmStatic
                  public val serviceDescriptor: ServiceDescriptor
                    get() = EventsGrpc.getServiceDescriptor()

                  public val sendEventMethod: MethodDescriptor<SendEventRequest, SendEventResponse>
                    @JvmStatic
                    get() = EventsGrpc.getSendEventMethod()

                  @StubFor(EventsGrpc::class)
                  public class EventsCoroutineStub @JvmOverloads constructor(
                    channel: Channel,
                    callOptions: CallOptions = DEFAULT,
                  ) : AbstractCoroutineStub<EventsCoroutineStub>(channel, callOptions) {
                    public override fun build(channel: Channel, callOptions: CallOptions): EventsCoroutineStub =
                        EventsCoroutineStub(channel, callOptions)

                    public suspend fun sendEvent(request: SendEventRequest, headers: Metadata = Metadata()):
                        SendEventResponse = unaryRpc(
                      channel,
                      EventsGrpc.getSendEventMethod(),
                      request,
                      callOptions,
                      headers
                    )
                  }

                  public abstract class EventsCoroutineImplBase(
                    coroutineContext: CoroutineContext = EmptyCoroutineContext,
                  ) : AbstractCoroutineServerImpl(coroutineContext) {
                    public open suspend fun sendEvent(request: SendEventRequest): SendEventResponse = throw
                        StatusException(UNIMPLEMENTED.withDescription("Method Events.sendEvent is unimplemented"))

                    public final override fun bindService(): ServerServiceDefinition =
                        builder(getServiceDescriptor())
                      .addMethod(unaryServerMethodDefinition(
                      context = this.context,
                      descriptor = EventsGrpc.getSendEventMethod(),
                      implementation = ::sendEvent
                    )).build()
                  }
                }
            """.trimIndent())
    }

}
