package com.blackink.app.di

import org.koin.core.module.Module

/** Platform-specific settings storage bindings. */
expect fun platformModule(): Module
