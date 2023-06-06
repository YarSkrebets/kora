package ru.tinkoff.grpc.client;

import io.grpc.ManagedChannel;
import io.grpc.ServiceDescriptor;
import reactor.core.publisher.Mono;
import ru.tinkoff.grpc.client.config.GrpcClientConfig;
import ru.tinkoff.grpc.client.config.GrpcClientConfigInterceptor;
import ru.tinkoff.grpc.client.telemetry.GrpcClientTelemetry;
import ru.tinkoff.grpc.client.telemetry.GrpcClientTelemetryFactory;
import ru.tinkoff.grpc.client.telemetry.TelemetryInterceptor;
import ru.tinkoff.kora.application.graph.Lifecycle;
import ru.tinkoff.kora.application.graph.Wrapped;

import java.net.URI;
import java.util.Objects;

public final class ManagedChannelLifecycle implements Lifecycle, Wrapped<ManagedChannel> {
    private final GrpcClientConfig config;
    private final GrpcClientTelemetry telemetry;
    private final ServiceDescriptor serviceDefinition;
    private final GrpcClientChannelFactory channelFactory;
    private volatile ManagedChannel channel;

    public ManagedChannelLifecycle(GrpcClientConfig config, ServiceDescriptor serviceDefinition, GrpcClientTelemetryFactory telemetryFactory, GrpcClientChannelFactory channelFactory) {
        this.config = config;
        this.serviceDefinition = serviceDefinition;
        this.channelFactory = channelFactory;
        this.telemetry = telemetryFactory.get(serviceDefinition);
    }

    @Override
    public Mono<?> init() {
        return Mono.fromRunnable(() -> {
            var url = URI.create(this.config.url());
            var host = url.getHost();
            var port = url.getPort();
            var scheme = url.getScheme();
            if (port < 0) {
                if (Objects.equals(scheme, "http")) {
                    port = 80;
                } else if (Objects.equals(scheme, "https")) {
                    port = 443;
                } else {
                    throw new IllegalArgumentException("Unknown scheme '" + scheme + "'");
                }
            }
            var b = this.channelFactory.forAddress(host, port);
            if (Objects.equals(scheme, "http")) {
                b.usePlaintext();
            }
            b.intercept(
                new TelemetryInterceptor(this.telemetry),
                new GrpcClientConfigInterceptor(this.config)
            );
            this.channel = b.build();
        });
    }

    @Override
    public Mono<?> release() {
        return Mono.fromCallable(() -> {
            var channel = this.channel;
            this.channel = null;
            if (channel != null) {
                this.channel.shutdown();
            }
            return null;
        });
    }

    @Override
    public ManagedChannel value() {
        return this.channel;
    }
}
