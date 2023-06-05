package ru.tinkoff.grpc.client.telemetry;

import ru.tinkoff.kora.common.Context;

public interface GrpcClientTelemetry {
    GrpcClientTelemetryCtx get(Context ctx);

    interface GrpcClientTelemetryCtx {

    }
}
