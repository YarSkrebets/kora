package ru.tinkoff.grpc.client.telemetry;

import io.grpc.Metadata;
import io.grpc.Status;
import ru.tinkoff.kora.common.Context;

import javax.annotation.Nullable;

public interface GrpcClientTelemetry {
    GrpcClientTelemetryContext get(Context ctx);

    interface GrpcClientTelemetryContext {
        void startSendMessage(Metadata sourceHeaders);

        void finishMessage(Metadata trailers, Status status, @Nullable Exception exception);
    }
}
