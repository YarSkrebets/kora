package ru.tinkoff.kora.grpc.server.config;

import javax.annotation.Nullable;

public record GrpcServerConfig(int port) {

    public GrpcServerConfig(@Nullable Integer port) {
        this(
            port != null ? port : 8090
        );
    }
}
