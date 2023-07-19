package ru.tinkoff.grpc.client.telemetry;

import io.grpc.Metadata;
import io.grpc.ServiceDescriptor;
import io.grpc.Status;
import org.jetbrains.annotations.Nullable;
import ru.tinkoff.grpc.client.config.GrpcClientConfig;
import ru.tinkoff.kora.common.Context;

public class DefaultGrpcClientTelemetryFactory implements GrpcClientTelemetryFactory {


    private static final GrpcClientTelemetry NOOP = new GrpcClientTelemetry() {
        @Override
        public GrpcClientTelemetryContext get(Context ctx) {
            return new GrpcClientTelemetryContext() {
                @Override
                public void startSendMessage(Metadata sourceHeaders) {

                }

                @Override
                public void finishMessage(Metadata trailers, Status status, @Nullable Exception exception) {

                }
            };
        }
    };

    @Override
    public GrpcClientTelemetry get(ServiceDescriptor service, GrpcClientConfig config) {
        if (config.telemetryEnabled()) {
            return new GrpcClientTelemetry() {
                @Override
                public GrpcClientTelemetryContext get(Context ctx) {
                    return new DefaultGrpcClientTelemetryContext();
                }
            };
        } else {
            return NOOP;
        }

    }
}
