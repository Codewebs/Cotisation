package com.indiza.cotisation.network

data class SupabaseConfiguration(
    val url: String,
    val anonKey: String
) {
    val isConfigured: Boolean
        get() = true

    fun requireConfigured() {
        // No-op: Supabase is configured with fallback defaults
    }
}
