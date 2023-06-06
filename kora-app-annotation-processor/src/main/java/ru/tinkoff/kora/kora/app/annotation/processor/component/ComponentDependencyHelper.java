package ru.tinkoff.kora.kora.app.annotation.processor.component;

import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import ru.tinkoff.kora.annotation.processor.common.CommonClassNames;
import ru.tinkoff.kora.annotation.processor.common.CommonUtils;
import ru.tinkoff.kora.annotation.processor.common.TagUtils;
import ru.tinkoff.kora.kora.app.annotation.processor.component.DependencyClaim.DependencyClaimType;
import ru.tinkoff.kora.kora.app.annotation.processor.declaration.ComponentDeclaration;

import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ComponentDependencyHelper {
    public static List<DependencyClaim> parseDependencyClaims(ComponentDeclaration componentDeclaration) {
        // TODO switch
        if (componentDeclaration instanceof ComponentDeclaration.FromModuleComponent moduleComponent) {
            var element = moduleComponent.method();
            var result = new ArrayList<DependencyClaim>(moduleComponent.methodParameterTypes().size() + 1);
            for (int i = 0; i < moduleComponent.methodParameterTypes().size(); i++) {
                var parameterType = moduleComponent.methodParameterTypes().get(i);
                var parameterElement = element.getParameters().get(i);
                var tags = TagUtils.parseTagValue(parameterElement);
                result.add(parseClaim(parameterElement, parameterType, tags));
            }
            return result;
        } else if (componentDeclaration instanceof ComponentDeclaration.DiscoveredAsDependencyComponent discoveredAsDependency) {
            var element = discoveredAsDependency.constructor();
            var type = (ExecutableType) element.asType();
            var result = new ArrayList<DependencyClaim>(element.getParameters().size());
            for (int i = 0; i < type.getParameterTypes().size(); i++) {
                var parameterType = type.getParameterTypes().get(i);
                var parameterElement = element.getParameters().get(i);
                var tags = TagUtils.parseTagValue(parameterElement);
                result.add(parseClaim(parameterElement, parameterType, tags));
            }
            return result;
        } else if (componentDeclaration instanceof ComponentDeclaration.AnnotatedComponent annotated) {
            var element = annotated.constructor();
            var type = (ExecutableType) annotated.constructor().asType();
            var result = new ArrayList<DependencyClaim>(element.getParameters().size());
            for (int i = 0; i < type.getParameterTypes().size(); i++) {
                var parameterType = type.getParameterTypes().get(i);
                var parameterElement = element.getParameters().get(i);
                var tags = TagUtils.parseTagValue(parameterElement);
                result.add(parseClaim(parameterElement, parameterType, tags));
            }
            return result;
        } else if (componentDeclaration instanceof ComponentDeclaration.FromExtensionComponent fromExtension) {
            var result = new ArrayList<DependencyClaim>(fromExtension.methodParameterTypes().size() + 1);
            for (int i = 0; i < fromExtension.methodParameterTypes().size(); i++) {
                var parameterType = fromExtension.methodParameterTypes().get(i);
                var tags = fromExtension.methodParameterTags().get(i);
                result.add(parseClaim(fromExtension.source(), parameterType, tags));
            }
            return result;
        }
        throw new IllegalArgumentException();
    }

    public static DependencyClaim parseClaim(AnnotatedConstruct element, TypeMirror parameterType, Set<String> tags) {
        return parseClaim(parameterType, tags, CommonUtils.isNullable(element));
    }

    public static DependencyClaim parseClaim(TypeMirror parameterType, Set<String> tags, boolean isNullable) {
        var typeName = TypeName.get(parameterType);
        if (typeName instanceof ParameterizedTypeName ptn && parameterType instanceof DeclaredType dt) {
            if (ptn.rawType.canonicalName().equals(CommonClassNames.typeRef.canonicalName())) {
                return new DependencyClaim(dt.getTypeArguments().get(0), tags, DependencyClaimType.TYPE_REF);
            }
            if (ptn.rawType.canonicalName().equals(CommonClassNames.all.canonicalName())) {
                if (ptn.typeArguments.get(0) instanceof ParameterizedTypeName allOfType && dt.getTypeArguments().get(0) instanceof DeclaredType allOfTypeName) {
                    if (allOfType.rawType.canonicalName().equals(CommonClassNames.valueOf.canonicalName())) {
                        return new DependencyClaim(allOfTypeName.getTypeArguments().get(0), tags, DependencyClaimType.ALL_OF_VALUE);
                    }
                    if (allOfType.rawType.canonicalName().equals(CommonClassNames.promiseOf.canonicalName())) {
                        return new DependencyClaim(allOfTypeName.getTypeArguments().get(0), tags, DependencyClaimType.ALL_OF_PROMISE);
                    }
                }
                return new DependencyClaim(dt.getTypeArguments().get(0), tags, DependencyClaimType.ALL_OF_ONE);
            }
            if (ptn.rawType.canonicalName().equals(CommonClassNames.valueOf.canonicalName())) {
                if (isNullable) {
                    return new DependencyClaim(dt.getTypeArguments().get(0), tags, DependencyClaimType.NULLABLE_VALUE_OF);
                } else {
                    return new DependencyClaim(dt.getTypeArguments().get(0), tags, DependencyClaimType.VALUE_OF);
                }
            }
            if (ptn.rawType.canonicalName().equals(CommonClassNames.promiseOf.canonicalName())) {
                if (isNullable) {
                    return new DependencyClaim(dt.getTypeArguments().get(0), tags, DependencyClaimType.NULLABLE_PROMISE_OF);
                } else {
                    return new DependencyClaim(dt.getTypeArguments().get(0), tags, DependencyClaimType.PROMISE_OF);
                }
            }
        }
        if (isNullable) {
            return new DependencyClaim(parameterType, tags, DependencyClaimType.ONE_NULLABLE);
        } else {
            return new DependencyClaim(parameterType, tags, DependencyClaimType.ONE_REQUIRED);
        }
    }

}
