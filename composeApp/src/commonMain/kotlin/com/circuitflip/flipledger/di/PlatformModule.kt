package com.circuitflip.flipledger.di

import org.koin.core.module.Module

/** Platform-specific settings storage bindings. */
expect fun platformModule(): Module
