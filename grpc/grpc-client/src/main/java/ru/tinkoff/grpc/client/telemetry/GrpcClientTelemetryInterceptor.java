package ru.tinkoff.grpc.client.telemetry;

import io.grpc.*;
import org.jetbrains.annotations.Nullable;
import ru.tinkoff.kora.common.Context;

public final class GrpcClientTelemetryInterceptor implements ClientInterceptor {
    private final GrpcClientTelemetry telemetry;

    public GrpcClientTelemetryInterceptor(GrpcClientTelemetry telemetry) {
        this.telemetry = telemetry;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        var ctx = Context.current();
        var grpcCtx = this.telemetry.get(ctx);

        var call = next.newCall(method, callOptions);

        return new MyClientCall<>(call, grpcCtx);
    }

    private static final class MyClientCall<ReqT, RespT> extends ClientCall<ReqT, RespT> {
        private final ClientCall<ReqT, RespT> delegate;
        private final GrpcClientTelemetry.GrpcClientTelemetryContext telemetryContext;

        private MyClientCall(ClientCall<ReqT, RespT> delegate, GrpcClientTelemetry.GrpcClientTelemetryContext telemetryContext) {
            this.delegate = delegate;
            this.telemetryContext = telemetryContext;
        }

        @Override
        public void start(Listener<RespT> responseListener, Metadata headers) {
            // todo: make span, fill headers
            var patchedHeaders = telemetryContext.startSendMessage(headers);
            delegate.start(new MyListener<>(responseListener, telemetryContext), patchedHeaders);
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
        private final GrpcClientTelemetry.GrpcClientTelemetryContext telemetryContext;

        public MyListener(ClientCall.Listener<RespT> responseListener, GrpcClientTelemetry.GrpcClientTelemetryContext telemetryContext) {
            this.responseListener = responseListener;
            this.telemetryContext = telemetryContext;
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
            telemetryContext.finishMessage(trailers, status, null);
            // todo: try read headers, if failed(or status failed) fail span.
            responseListener.onClose(status, trailers);
        }

        @Override
        public void onReady() {
            responseListener.onReady();
        }
    }
}
