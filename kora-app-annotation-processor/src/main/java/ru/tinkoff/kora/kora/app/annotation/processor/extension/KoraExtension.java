package ru.tinkoff.kora.kora.app.annotation.processor.extension;

import ru.tinkoff.kora.annotation.processor.common.CommonUtils;

import javax.annotation.Nullable;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.io.IOException;
import java.util.Set;

public interface KoraExtension {
    @Nullable
    default KoraExtensionDependencyGenerator getDependencyGenerator(RoundEnvironment roundEnvironment, TypeMirror typeMirror, Set<String> tag) {
        if (tag.isEmpty()) {
            return getDependencyGenerator(roundEnvironment, typeMirror);
        }
        return null;
    }

    @Nullable
    default KoraExtensionDependencyGenerator getDependencyGenerator(RoundEnvironment roundEnvironment, TypeMirror typeMirror) {
        return getDependencyGenerator(roundEnvironment, typeMirror, Set.of());
    }

    interface KoraExtensionDependencyGenerator {
        ExtensionResult generateDependency() throws IOException;

        static KoraExtensionDependencyGenerator generatedFrom(Elements elements, Element element, String postfix) {
            var mapperName = CommonUtils.getOuterClassesAsPrefix(element) + element.getSimpleName() + postfix;
            var packageElement = elements.getPackageOf(element);

            return () -> {
                var maybeGenerated = elements.getTypeElement(packageElement.getQualifiedName() + "." + mapperName);
                if (maybeGenerated != null) {
                    var constructors = CommonUtils.findConstructors(maybeGenerated, m -> m.contains(Modifier.PUBLIC));
                    if (constructors.size() != 1) throw new IllegalStateException();
                    return ExtensionResult.fromExecutable(constructors.get(0));
                }
                return ExtensionResult.nextRound();
            };
        }
    }
}
