package com.circuitflip.flipledger.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json

/**
 * Builds the shared [SupabaseClient]. Installs Auth (email/password sessions) and Postgrest
 * (the REST data API over our tables). A custom JSON serializer is used so that:
 *  - `encodeDefaults = true` → every DTO field is written on insert/upsert (no silently
 *    dropped columns), and
 *  - `ignoreUnknownKeys = true` → server-managed columns (`user_id`, `created_at`, …) that
 *    aren't in our DTOs don't break decoding.
 */
@OptIn(SupabaseInternal::class)
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
        // Ktor 3.0.x's Darwin engine validates a decompressed iOS response against the
        // compressed Content-Length reported by the server (KTOR-7943). Supabase error
        // responses can therefore look like failed requests even after the RPC ran.
        // Request the original representation until the networking stack is upgraded.
        httpConfig {
            defaultRequest {
                header(HttpHeaders.AcceptEncoding, "identity")
            }
        }
        install(Auth)
        install(Postgrest)
    }
