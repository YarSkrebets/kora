package ru.tinkoff.grpc.client;

import io.netty.channel.EventLoopGroup;
import ru.tinkoff.grpc.client.telemetry.DefaultGrpcClientTelemetryFactory;
import ru.tinkoff.kora.common.DefaultComponent;
import ru.tinkoff.kora.netty.common.NettyCommonModule;

public interface GrpcNettyClientModule extends NettyCommonModule {
    @DefaultComponent
    default DefaultGrpcClientTelemetryFactory defaultGrpcClientTelemetryFactory() {
        return new DefaultGrpcClientTelemetryFactory();
    }

    @DefaultComponent
    default GrpcNettyClientChannelFactory grpcNettyClientChannelFactory(EventLoopGroup eventLoopGroup) {
        return new GrpcNettyClientChannelFactory(eventLoopGroup);
    }
}
