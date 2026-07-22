package com.circuitflip.flipledger.di

import org.koin.core.module.Module

/** Platform-specific bindings (settings storage, DB driver factory). */
expect fun platformModule(): Module
