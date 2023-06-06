package ru.tinkoff.grpc.client.telemetry;

import io.grpc.ServiceDescriptor;

public interface GrpcClientTelemetryFactory {
    GrpcClientTelemetry get(ServiceDescriptor service);
}
