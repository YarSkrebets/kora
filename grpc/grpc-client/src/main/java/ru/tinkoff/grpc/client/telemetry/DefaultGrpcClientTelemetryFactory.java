package ru.tinkoff.grpc.client.telemetry;

import io.grpc.ServiceDescriptor;
import ru.tinkoff.grpc.client.config.GrpcClientConfig;
import ru.tinkoff.kora.common.Context;

public class DefaultGrpcClientTelemetryFactory implements GrpcClientTelemetryFactory {
    @Override
    public GrpcClientTelemetry get(ServiceDescriptor service, GrpcClientConfig config) {
        return new GrpcClientTelemetry() {
            @Override
            public GrpcClientTelemetryCtx get(Context ctx) {
                return new GrpcClientTelemetryCtx() {};
            }
        };
    }
}
