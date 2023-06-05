package ru.tinkoff.grpc.client;

import ru.tinkoff.kora.kora.app.annotation.processor.extension.KoraExtension;

import javax.annotation.Nullable;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.type.TypeMirror;
import java.util.Set;

public final class GrpcClientExtension implements KoraExtension {

    @Nullable
    @Override
    public KoraExtensionDependencyGenerator getDependencyGenerator(RoundEnvironment roundEnvironment, TypeMirror typeMirror, Set<String> tag) {
        return KoraExtension.super.getDependencyGenerator(roundEnvironment, typeMirror, tag);
    }
}
