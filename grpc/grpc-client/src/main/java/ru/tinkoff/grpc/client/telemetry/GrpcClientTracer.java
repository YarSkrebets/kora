package ru.tinkoff.grpc.client.telemetry;

import io.grpc.Metadata;
import io.grpc.Status;

public interface GrpcClientTracer {
    interface Span {
        void onSend(Object message);

        void onReceive(Object message);

        void close(Status status, Throwable throwable);
    }

    Span createSpan(Metadata headers);
}
