package com.blackink.app.domain.model

/**
 * Plan entitlements / limits. Currently everyone is on the Free plan; when Stripe billing is
 * added, gate these on the subscription status (e.g. exempt Pro users from [FREE_DEVICE_LIMIT]).
 */
object Entitlements {
    /** Maximum active inventory devices allowed on the Free plan. */
    const val FREE_DEVICE_LIMIT = 5
}
