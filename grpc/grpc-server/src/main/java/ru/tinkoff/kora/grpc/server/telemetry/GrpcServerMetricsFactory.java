package ru.tinkoff.kora.grpc.server.telemetry;

import io.grpc.Metadata;
import io.grpc.ServerCall;

public interface GrpcServerMetricsFactory {
    GrpcServerMetrics get(ServerCall<?, ?> call, Metadata headers, String serviceName, String methodName);
}
