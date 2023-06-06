package ru.tinkoff.grpc.client;


import org.assertj.core.api.Assertions;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import ru.tinkoff.kora.annotation.processor.common.AbstractAnnotationProcessorTest;
import ru.tinkoff.kora.kora.app.annotation.processor.KoraAppProcessor;

import java.util.Arrays;
import java.util.List;

class GrpcClientExtensionTest extends AbstractAnnotationProcessorTest {

    protected void compile(@Language("java") String... sources) {
        var patchedSources = Arrays.copyOf(sources, sources.length + 1);
        patchedSources[sources.length] = """
            @ru.tinkoff.kora.common.Module
            public interface ConfigModule extends ru.tinkoff.grpc.client.GrpcNettyClientModule {
              default com.typesafe.config.Config config() {
                return com.typesafe.config.ConfigFactory.parseMap(java.util.Map.of(
                  "testPath", java.util.Map.of(
                    "url", "http://localhost:8080",
                    "timeout", "20s"
                  )
                ));
              }
            }
            """;

        super.compile(List.of(new KoraAppProcessor()), patchedSources);

        compileResult.assertSuccess();
        try (var g = loadGraph("TestApp");) {
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
            Assertions.assertThat(g.draw().size()).isEqualTo(10);
        }
    }


    @Test
    public void testBlockingStub() {
        compile("""
            @KoraApp
            public interface TestApp {
              @Tag(ru.tinkoff.kora.grpc.server.events.EventsGrpc.class)
              default ru.tinkoff.grpc.client.config.GrpcClientConfig config(com.typesafe.config.Config config) {
                return ru.tinkoff.grpc.client.config.GrpcClientConfig.fromConfig(config, "testPath");
              }
                        
              @Root
              default String test(ru.tinkoff.kora.grpc.server.events.EventsGrpc.EventsBlockingStub stub) {
                return "";
              }
            }
            """);
    }

    @Test
    public void testFutureStub() {
        compile("""
            @KoraApp
            public interface TestApp {
                        
              @Tag(ru.tinkoff.kora.grpc.server.events.EventsGrpc.class)
              default ru.tinkoff.grpc.client.config.GrpcClientConfig config(com.typesafe.config.Config config) {
                return ru.tinkoff.grpc.client.config.GrpcClientConfig.fromConfig(config, "testPath");
              }
                        
              @Root
              default String test(ru.tinkoff.kora.grpc.server.events.EventsGrpc.EventsFutureStub stub) {
                return "";
              }
            }
            """);
    }

    @Test
    public void testAsyncStub() {
        compile("""
            @KoraApp
            public interface TestApp {
                        
              @Tag(ru.tinkoff.kora.grpc.server.events.EventsGrpc.class)
              default ru.tinkoff.grpc.client.config.GrpcClientConfig config(com.typesafe.config.Config config) {
                return ru.tinkoff.grpc.client.config.GrpcClientConfig.fromConfig(config, "testPath");
              }
                        
              @Root
              default String test(ru.tinkoff.kora.grpc.server.events.EventsGrpc.EventsStub stub) {
                return "";
              }
            }
            """);
    }
}
