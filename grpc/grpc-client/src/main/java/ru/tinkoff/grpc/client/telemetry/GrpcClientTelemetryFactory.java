package ru.tinkoff.grpc.client.telemetry;

import io.grpc.ServiceDescriptor;
import ru.tinkoff.grpc.client.config.GrpcClientConfig;

public interface GrpcClientTelemetryFactory {
    GrpcClientTelemetry get(ServiceDescriptor service, GrpcClientConfig config);
}
