package ru.tinkoff.kora.ksp.common

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeSpec

object TagUtils {
    val ignoreList = setOf("Component", "DefaultComponent")

    fun Set<String>.toTagAnnotation(): AnnotationSpec = AnnotationSpec.builder(CommonClassNames.tag).let { builder ->
        this.forEach { tag -> builder.addMember("%L::class", tag) }
        builder.build()
    }

    fun KSAnnotated.parseTags(): Set<String> {
        return parseTagValue(this)
    }

    fun FunSpec.Builder.addTag(tag: Set<String>): FunSpec.Builder {
        if (tag.isEmpty()) {
            return this
        }
        return this.addAnnotation(tag.toTagAnnotation())
    }

    fun ParameterSpec.Builder.addTag(tag: Set<String>): ParameterSpec.Builder {
        if (tag.isEmpty()) {
            return this
        }
        return this.addAnnotation(tag.toTagAnnotation())
    }

    fun TypeSpec.Builder.addTag(tag: Set<String>): TypeSpec.Builder {
        if (tag.isEmpty()) {
            return this
        }
        return this.addAnnotation(tag.toTagAnnotation())
    }

    fun parseTagValue(target: KSAnnotated): Set<String> {
        for (annotation in target.annotations.filter { !ignoreList.contains(it.shortName.asString()) }) {
            val type = annotation.annotationType.resolve()
            if (type.declaration.qualifiedName!!.asString() == CommonClassNames.tag.canonicalName) {
                return AnnotationUtils.parseAnnotationValueWithoutDefaults<List<KSType>>(annotation, "value")!!
                    .asSequence()
                    .map { it.declaration.qualifiedName!!.asString() }
                    .toSet()
            }
            for (annotatedWith in type.declaration.annotations) {
                val type = annotatedWith.annotationType.resolve()
                if (type.declaration.qualifiedName!!.asString() == CommonClassNames.tag.canonicalName) {
                    return AnnotationUtils.parseAnnotationValueWithoutDefaults<List<KSType>>(annotatedWith, "value")!!
                        .asSequence()
                        .map { it.declaration.qualifiedName!!.asString() }
                        .toSet()
                }

            }
        }
        return setOf()
    }
}
