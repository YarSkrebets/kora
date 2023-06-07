package ru.tinkoff.grpc.client.ksp

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import ru.tinkoff.grpc.client.ksp.GrpcClassNames.channel
import ru.tinkoff.grpc.client.ksp.GrpcClassNames.grpcGenerated
import ru.tinkoff.grpc.client.ksp.GrpcClassNames.managedChannelLifecycle
import ru.tinkoff.grpc.client.ksp.GrpcClassNames.serviceDescriptor
import ru.tinkoff.grpc.client.ksp.GrpcClassNames.stubFor
import ru.tinkoff.kora.kora.app.ksp.extension.ExtensionResult
import ru.tinkoff.kora.kora.app.ksp.extension.ExtensionResult.CodeBlockResult
import ru.tinkoff.kora.kora.app.ksp.extension.KoraExtension
import ru.tinkoff.kora.ksp.common.AnnotationUtils.findAnnotation
import ru.tinkoff.kora.ksp.common.AnnotationUtils.findValueNoDefault
import ru.tinkoff.kora.ksp.common.AnnotationUtils.isAnnotationPresent
import ru.tinkoff.kora.ksp.common.KspCommonUtils.getClassDeclarationByName

class GrpcClientExtension(
    val resolver: Resolver,
    val kspLogger: KSPLogger,
    val codeGenerator: CodeGenerator,
    val abstractStub: KSClassDeclaration?,
    val abstractCoroutineStub: KSClassDeclaration?) : KoraExtension {

    override fun getDependencyGenerator(resolver: Resolver, type: KSType, tag: Set<String>): (() -> ExtensionResult)? {
        if (abstractStub != null && abstractStub.asStarProjectedType().isAssignableFrom(type)) {
            return this.generateJavaStub(type, tag)
        }
        if (abstractCoroutineStub != null && abstractCoroutineStub.asStarProjectedType().isAssignableFrom(type)) {
            return this.generateCoroutineStub(type, tag)
        }
        val typeName = type.toTypeName()
        if (typeName == channel) {
            return this.generateChannel(type, tag)
        }
        if (typeName == serviceDescriptor) {
            return this.generateGrpcServiceDescriptor(type, tag)
        }
        return null
    }

    private fun generateJavaStub(type: KSType, tag: Set<String>): (() -> ExtensionResult)? {
        if (tag.isNotEmpty()) {
            return null
        }
        val classDecl = type.declaration
        if (classDecl !is KSClassDeclaration) {
            return null
        }
        val apiClassDecl = classDecl.parentDeclaration
        if (apiClassDecl !is KSClassDeclaration) {
            return null
        }
        val apiClassName = apiClassDecl.toClassName()
        if (!apiClassDecl.isAnnotationPresent(grpcGenerated)) {
            return null
        }
        val typeName = classDecl.simpleName.asString()
        val sourceElement = when {
            typeName.endsWith("BlockingStub") -> this.findMethod(apiClassDecl, "newBlockingStub")
            typeName.endsWith("FutureStub") -> this.findMethod(apiClassDecl, "newFutureStub")
            else -> this.findMethod(apiClassDecl, "newStub")
        }
        val channelType = sourceElement.parameters[0].type.resolve()
        return {
            CodeBlockResult(
                sourceElement,
                { CodeBlock.of("%T.%N(%L)", apiClassName, sourceElement.simpleName.asString(), it) },
                type,
                tag,
                listOf(channelType),
                listOf(setOf(apiClassName.canonicalName))
            )
        }
    }

    private fun findMethod(apiClassDecl: KSClassDeclaration, name: String) = apiClassDecl.getDeclaredFunctions()
        .filter { it.simpleName.asString() == name }
        .first()

    private fun generateGrpcServiceDescriptor(type: KSType, tag: Set<String>): (() -> ExtensionResult)? {
        if (tag.isEmpty()) {
            return null
        }
        val serviceDescriptorDecl = resolver.getClassDeclarationByName(serviceDescriptor)!!
        val serviceDescriptorType = serviceDescriptorDecl.asStarProjectedType()

        val grpcServiceDecl = resolver.getClassDeclarationByName(tag.first())!!
        val grpcServiceClassName = grpcServiceDecl.toClassName()
        return {
            CodeBlockResult(
                grpcServiceDecl,
                { CodeBlock.of("%T.getServiceDescriptor()", grpcServiceClassName) },
                serviceDescriptorType,
                tag,
                listOf(),
                listOf()
            )
        }
    }

    private fun generateChannel(type: KSType, tag: Set<String>): (() -> ExtensionResult)? {
        if (tag.isEmpty()) {
            return null
        }
        val managedChannelDecl = resolver.getClassDeclarationByName(managedChannelLifecycle.canonicalName)!!
        val managedChannelType = managedChannelDecl.asStarProjectedType()
        val managedChannelConstructor = managedChannelDecl.getConstructors().first()
        val parameterTags = arrayListOf<Set<String>>()
        val parameterTypes = arrayListOf<KSType>()
        for ((i, parameter) in managedChannelConstructor.parameters.withIndex()) {
            if (i < 3) {
                parameterTags.add(tag)
            } else {
                parameterTags.add(setOf())
            }
            parameterTypes.add(parameter.type.resolve())
        }
        return {
            CodeBlockResult(
                managedChannelConstructor,
                { CodeBlock.of("%T(%L)", managedChannelLifecycle, it) },
                managedChannelType,
                tag,
                parameterTypes,
                parameterTags
            )
        }
    }


    private fun generateCoroutineStub(type: KSType, tag: Set<String>): (() -> ExtensionResult)? {
        if (tag.isNotEmpty()) {
            return null
        }
        val classDecl = type.declaration
        if (classDecl !is KSClassDeclaration) {
            return null
        }
        val apiClassDecl = classDecl.parentDeclaration
        if (apiClassDecl !is KSClassDeclaration) {
            return null
        }
        val stubForAnnotation = apiClassDecl.findAnnotation(stubFor)
        if (stubForAnnotation == null) {
            return null
        }
        val apiTag = stubForAnnotation.findValueNoDefault<List<KSType>>("value")!!
            .map { it.toClassName().canonicalName }
            .toSet()
        val implClassName = classDecl.toClassName()
        val constructor = classDecl.primaryConstructor!!
        val channelType = constructor.parameters[0].type.resolve()
        return {
            CodeBlockResult(
                classDecl,
                { CodeBlock.of("%T(%L)", implClassName, it) },
                type,
                apiTag,
                listOf(channelType),
                listOf(apiTag)
            )
        }
    }
}

