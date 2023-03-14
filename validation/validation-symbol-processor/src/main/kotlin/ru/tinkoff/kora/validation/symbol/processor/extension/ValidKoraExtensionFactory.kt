package ru.tinkoff.kora.validation.symbol.processor.extension

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import ru.tinkoff.kora.kora.app.ksp.extension.ExtensionFactory
import ru.tinkoff.kora.kora.app.ksp.extension.KoraExtension

class ValidKoraExtensionFactory : ExtensionFactory {
    override fun create(resolver: Resolver, kspLogger: KSPLogger, codeGenerator: CodeGenerator): KoraExtension? {
        val json = resolver.getClassDeclarationByName("ru.tinkoff.kora.validation.common.annotation.Validated")
        return if (json == null) {
            null
        } else {
            ValidKoraExtension(resolver)
        }
    }
}