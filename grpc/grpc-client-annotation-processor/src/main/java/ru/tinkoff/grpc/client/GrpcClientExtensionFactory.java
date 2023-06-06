package ru.tinkoff.grpc.client;

import ru.tinkoff.kora.kora.app.annotation.processor.extension.ExtensionFactory;
import ru.tinkoff.kora.kora.app.annotation.processor.extension.KoraExtension;

import javax.annotation.processing.ProcessingEnvironment;
import java.util.Optional;

import static ru.tinkoff.grpc.client.GrpcClassNames.abstractStub;

public class GrpcClientExtensionFactory implements ExtensionFactory {
    @Override
    public Optional<KoraExtension> create(ProcessingEnvironment processingEnvironment) {
        var stubTypeElement = processingEnvironment.getElementUtils().getTypeElement(abstractStub.canonicalName());
        if (stubTypeElement == null) {
            return Optional.empty();
        }
        var stubErasure = processingEnvironment.getTypeUtils().erasure(stubTypeElement.asType());
        return Optional.of(new GrpcClientExtension(processingEnvironment, stubTypeElement, stubErasure));
    }
}
