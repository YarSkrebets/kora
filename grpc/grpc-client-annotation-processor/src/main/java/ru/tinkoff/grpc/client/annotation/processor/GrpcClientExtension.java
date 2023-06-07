package ru.tinkoff.grpc.client.annotation.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import ru.tinkoff.kora.annotation.processor.common.AnnotationUtils;
import ru.tinkoff.kora.annotation.processor.common.CommonUtils;
import ru.tinkoff.kora.kora.app.annotation.processor.extension.ExtensionResult;
import ru.tinkoff.kora.kora.app.annotation.processor.extension.KoraExtension;

import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static ru.tinkoff.grpc.client.annotation.processor.GrpcClassNames.*;

public final class GrpcClientExtension implements KoraExtension {
    private final ProcessingEnvironment env;
    private final TypeElement stubTypeElement;
    private final TypeMirror stubErasure;

    public GrpcClientExtension(ProcessingEnvironment env, TypeElement stubTypeElement, TypeMirror stubErasure) {
        this.env = env;
        this.stubTypeElement = stubTypeElement;
        this.stubErasure = stubErasure;
    }

    @Nullable
    @Override
    public KoraExtensionDependencyGenerator getDependencyGenerator(RoundEnvironment roundEnvironment, TypeMirror typeMirror, Set<String> tag) {
        if (stubTypeElement == null || stubErasure == null) {
            return null;
        }
        if (typeMirror.getKind() != TypeKind.DECLARED) {
            return null;
        }
        var dt = (DeclaredType) typeMirror;
        var te = (TypeElement) dt.asElement();
        var className = ClassName.get(te);
        if (env.getTypeUtils().isAssignable(typeMirror, stubErasure)) {
            return getStubGenerator(typeMirror, tag);
        }

        if (className.equals(channel) && tag.size() == 1) {
            return getChannel(typeMirror, tag);
        }
        if (className.equals(serviceDescriptor) && tag.size() == 1) {
            return getServiceDescriptor(typeMirror, tag);
        }

        return null;
    }

    private KoraExtensionDependencyGenerator getServiceDescriptor(TypeMirror typeMirror, Set<String> tag) {
        var serviceDescriptorTypeElement = env.getElementUtils().getTypeElement(serviceDescriptor.canonicalName());
        var serviceDescriptorTypeMirror = serviceDescriptorTypeElement.asType();

        var grpcServiceTypeElement = env.getElementUtils().getTypeElement(tag.iterator().next());
        var grpcServiceClassName = ClassName.get(grpcServiceTypeElement);

        return () -> new ExtensionResult.CodeBlockResult(
            grpcServiceTypeElement,
            params -> CodeBlock.of("$T.getServiceDescriptor()", grpcServiceClassName),
            serviceDescriptorTypeMirror,
            tag,
            List.of(),
            List.of()
        );
    }

    private KoraExtensionDependencyGenerator getChannel(TypeMirror typeMirror, Set<String> tag) {
        var managedChannelTypeElement = env.getElementUtils().getTypeElement(managedChannelLifecycle.canonicalName());
        var managedChannelTypeMirror = managedChannelTypeElement.asType();
        var managedChannelConstructor = CommonUtils.findConstructors(managedChannelTypeElement, m -> m.contains(Modifier.PUBLIC)).get(0);
        var parameterTags = new ArrayList<Set<String>>(managedChannelConstructor.getParameters().size());
        var parameterTypes = new ArrayList<TypeMirror>(managedChannelConstructor.getParameters().size());
        for (int i = 0; i < managedChannelConstructor.getParameters().size(); i++) {
            var parameter = managedChannelConstructor.getParameters().get(i);
            parameterTags.add(i < 3 ? tag : Set.of());
            parameterTypes.add(parameter.asType());
        }
        return () -> new ExtensionResult.CodeBlockResult(
            managedChannelConstructor,
            params -> CodeBlock.of("new $T($L)", managedChannelLifecycle, params),
            managedChannelTypeMirror,
            tag,
            parameterTypes,
            parameterTags
        );
    }

    @Nullable
    private KoraExtensionDependencyGenerator getStubGenerator(TypeMirror typeMirror, Set<String> tag) {
        if (!tag.isEmpty()) {
            return null;
        }
        var typeElement = env.getTypeUtils().asElement(typeMirror);
        var apiTypeElement = typeElement.getEnclosingElement();
        if (apiTypeElement.getKind() != ElementKind.CLASS) {
            return null;
        }
        var apiClassName = ClassName.get((TypeElement) apiTypeElement);
        if (AnnotationUtils.findAnnotation(apiTypeElement, grpcGenerated) == null) {
            return null;
        }
        var typeName = typeMirror.toString();
        final ExecutableElement sourceElement;
        if (typeName.endsWith("BlockingStub")) {
            sourceElement = this.findMethod(apiTypeElement, "newBlockingStub");
        } else if (typeName.endsWith("FutureStub")) {
            sourceElement = this.findMethod(apiTypeElement, "newFutureStub");
        } else {
            sourceElement = this.findMethod(apiTypeElement, "newStub");
        }
        var channelType = sourceElement.getParameters().get(0).asType();

        return () -> new ExtensionResult.CodeBlockResult(
            sourceElement,
            params -> CodeBlock.of("$T.$N($L)", apiClassName, sourceElement.getSimpleName(), params),
            typeMirror,
            tag,
            List.of(channelType),
            List.of(Set.of(apiClassName.canonicalName()))
        );
    }

    private ExecutableElement findMethod(Element apiTypeElement, String methodName) {
        for (var enclosedElement : apiTypeElement.getEnclosedElements()) {
            if (enclosedElement.getKind() != ElementKind.METHOD) {
                continue;
            }
            if (!enclosedElement.getModifiers().contains(Modifier.STATIC)) {
                continue;
            }
            if (!enclosedElement.getModifiers().contains(Modifier.PUBLIC)) {
                continue;
            }
            if (enclosedElement.getSimpleName().contentEquals(methodName)) {
                return (ExecutableElement) enclosedElement;
            }
        }
        throw new IllegalStateException();
    }
}
