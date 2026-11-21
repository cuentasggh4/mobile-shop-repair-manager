package com.example.util

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import java.util.Locale

/**
 * In-app language selection (no extra libraries). The chosen language is kept in
 * SharedPreferences and applied by wrapping the activity context in
 * MainActivity.attachBaseContext. Spanish (Mexico) is the default.
 */
object LanguageManager {

    const val DEFAULT_LANGUAGE = "es"
    const val DEFAULT_COUNTRY_CODE = "52"

    private const val PREFS = "language_prefs"
    private const val KEY_LANGUAGE = "app_language"
    private const val KEY_COUNTRY_CODE = "whatsapp_country_code"

    data class Option(val code: String, val nativeName: String)

    val options = listOf(
        Option("es", "Español"),
        Option("en", "English"),
        Option("hi", "हिन्दी"),
        Option("gu", "ગુજરાતી")
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getLanguage(context: Context): String {
        val saved = prefs(context).getString(KEY_LANGUAGE, null)
        return if (saved != null && options.any { it.code == saved }) saved else DEFAULT_LANGUAGE
    }

    fun setLanguage(context: Context, code: String) {
        prefs(context).edit().putString(KEY_LANGUAGE, code).apply()
    }

    /** Country calling code (digits only) added to customer numbers that have none. */
    fun getCountryCode(context: Context): String {
        val saved = prefs(context).getString(KEY_COUNTRY_CODE, null)
        return if (saved.isNullOrBlank()) DEFAULT_COUNTRY_CODE else saved
    }

    fun setCountryCode(context: Context, digits: String) {
        prefs(context).edit().putString(KEY_COUNTRY_CODE, digits.filter { it.isDigit() }).apply()
    }

    fun localeFor(code: String): Locale = when (code) {
        "es" -> Locale.Builder().setLanguage("es").setRegion("MX").build()
        "hi" -> Locale.Builder().setLanguage("hi").setRegion("IN").build()
        "gu" -> Locale.Builder().setLanguage("gu").setRegion("IN").build()
        else -> Locale.Builder().setLanguage("en").setRegion("US").build()
    }

    /** Returns a context whose resources use the selected language. */
    fun wrap(base: Context): Context {
        val locale = localeFor(getLanguage(base))
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }

    /** String lookup in the selected language, usable from any context (workers, view models, repositories). */
    fun getString(context: Context, @StringRes id: Int, vararg args: Any): String {
        val localized = wrap(context)
        return if (args.isEmpty()) localized.getString(id) else localized.getString(id, *args)
    }
}
