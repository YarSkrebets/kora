package ru.tinkoff.grpc.client.telemetry;

import io.grpc.*;
import org.jetbrains.annotations.Nullable;
import ru.tinkoff.kora.common.Context;

public final class TelemetryInterceptor implements ClientInterceptor {
    private final GrpcClientTelemetry telemetry;

    public TelemetryInterceptor(GrpcClientTelemetry telemetry) {
        this.telemetry = telemetry;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        var ctx = Context.clear();
        var grpcCtx = this.telemetry.get(ctx);

        var call = next.newCall(method, callOptions);

        return new MyClientCall<>(call, grpcCtx);
    }

    private static final class MyClientCall<ReqT, RespT> extends ClientCall<ReqT, RespT> {
        private final ClientCall<ReqT, RespT> delegate;
        private final GrpcClientTelemetry.GrpcClientTelemetryCtx telemetry;

        private MyClientCall(ClientCall<ReqT, RespT> delegate, GrpcClientTelemetry.GrpcClientTelemetryCtx telemetry) {
            this.delegate = delegate;
            this.telemetry = telemetry;
        }

        @Override
        public void start(Listener<RespT> responseListener, Metadata headers) {
            delegate.start(new MyListener<>(responseListener, telemetry), headers);
        }

        @Override
        public void request(int numMessages) {
            delegate.request(numMessages);
        }

        @Override
        public void cancel(@Nullable String message, @Nullable Throwable cause) {
            delegate.cancel(message, cause);
        }

        @Override
        public void halfClose() {
            delegate.halfClose();
        }

        @Override
        public void sendMessage(ReqT message) {
            delegate.sendMessage(message);
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setMessageCompression(boolean enabled) {
            delegate.setMessageCompression(enabled);
        }

        @Override
        public Attributes getAttributes() {
            return delegate.getAttributes();
        }
    }

    // TODO
    private static class MyListener<RespT> extends ClientCall.Listener<RespT> {
        private final ClientCall.Listener<RespT> responseListener;
        private final GrpcClientTelemetry.GrpcClientTelemetryCtx telemetry;

        public MyListener(ClientCall.Listener<RespT> responseListener, GrpcClientTelemetry.GrpcClientTelemetryCtx telemetry) {
            this.responseListener = responseListener;
            this.telemetry = telemetry;
        }

        @Override
        public void onHeaders(Metadata headers) {
            responseListener.onHeaders(headers);
        }

        @Override
        public void onMessage(RespT message) {
            responseListener.onMessage(message);
        }

        @Override
        public void onClose(Status status, Metadata trailers) {
            responseListener.onClose(status, trailers);
        }

        @Override
        public void onReady() {
            responseListener.onReady();
        }
    }
}
