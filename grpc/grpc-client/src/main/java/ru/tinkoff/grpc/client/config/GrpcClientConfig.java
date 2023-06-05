package ru.tinkoff.grpc.client.config;

import com.typesafe.config.Config;

import java.time.Duration;

public record GrpcClientConfig(String url, Duration timeout) {
    public static GrpcClientConfig fromConfig(Config config, String path) {
        var object = config.getObject(path).toConfig();

        var url = object.getString("url");
        var timeout = object.getDuration("timeout");

        return new GrpcClientConfig(url, timeout);
    }
}
