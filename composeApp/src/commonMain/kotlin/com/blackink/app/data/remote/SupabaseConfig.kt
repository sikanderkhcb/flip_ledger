package com.blackink.app.data.remote

/**
 * Supabase project connection details.
 *
 * The [ANON_KEY] is the project's public "anon" key — it is designed to be embedded in
 * client apps and is safe here; all data access is still gated by Row Level Security on
 * the server. The `service_role` key must NEVER appear in the app.
 */
object SupabaseConfig {
    const val URL = "https://owotsyqtaqznkxdhriuk.supabase.co"
    const val ANON_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im93b3RzeXF0YXF6bmt4ZGhyaXVrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODQ1NDcwNjMsImV4cCI6MjEwMDEyMzA2M30.3osw-nbOynbw4ld1sH6_gdRQYV_pHIGf8sbQp7OxOhA"
}
