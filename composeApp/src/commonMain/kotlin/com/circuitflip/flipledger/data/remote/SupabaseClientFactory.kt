package com.circuitflip.flipledger.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

/**
 * Builds the shared [SupabaseClient]. Installs Auth (email/password sessions) and Postgrest
 * (the REST data API over our tables). A custom JSON serializer is used so that:
 *  - `encodeDefaults = true` → every DTO field is written on insert/upsert (no silently
 *    dropped columns), and
 *  - `ignoreUnknownKeys = true` → server-managed columns (`user_id`, `created_at`, …) that
 *    aren't in our DTOs don't break decoding.
 */
fun createFlipLedgerSupabaseClient(): SupabaseClient =
    createSupabaseClient(
        supabaseUrl = SupabaseConfig.URL,
        supabaseKey = SupabaseConfig.ANON_KEY,
    ) {
        defaultSerializer = KotlinXSerializer(
            Json {
                encodeDefaults = true
                ignoreUnknownKeys = true
                isLenient = true
            },
        )
        install(Auth)
        install(Postgrest)
    }
