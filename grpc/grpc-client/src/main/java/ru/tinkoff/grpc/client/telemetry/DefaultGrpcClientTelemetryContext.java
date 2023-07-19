package ru.tinkoff.grpc.client.telemetry;

import io.grpc.Metadata;
import io.grpc.Status;
import org.jetbrains.annotations.Nullable;

public class DefaultGrpcClientTelemetryContext implements GrpcClientTelemetry.GrpcClientTelemetryContext {
    @Override
    public void startSendMessage(Metadata sourceHeaders) {

    }

    @Override
    public void finishMessage(Metadata trailers, Status status, @Nullable Exception exception) {

    }
}
