package com.academia.shared.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

/**
 * Initialize Koin for KMP shared module.
 */
fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(sharedModules)
}

/**
 * All shared Koin modules.
 */
val sharedModules: List<Module> = listOf(
    networkModule,
    // repositoryModule will be added later
    // useCaseModule will be added later
)
