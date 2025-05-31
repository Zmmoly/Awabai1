package com.awab.ai.utils

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "awab_ai_preferences"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_VOICE_ACTIVATION = "voice_activation"
        private const val KEY_AUTO_RESPONSE = "auto_response"
        private const val KEY_PREFERRED_LANGUAGE = "preferred_language"
        private const val KEY_RESPONSE_STYLE = "response_style"
    }
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    var isFirstLaunch: Boolean
        get() = sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_FIRST_LAUNCH, value).apply()
    
    var isVoiceActivationEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_VOICE_ACTIVATION, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_VOICE_ACTIVATION, value).apply()
    
    var isAutoResponseEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_AUTO_RESPONSE, false)
        set(value) = sharedPreferences.edit().putBoolean(KEY_AUTO_RESPONSE, value).apply()
    
    var preferredLanguage: String
        get() = sharedPreferences.getString(KEY_PREFERRED_LANGUAGE, "ar") ?: "ar"
        set(value) = sharedPreferences.edit().putString(KEY_PREFERRED_LANGUAGE, value).apply()
    
    var responseStyle: String
        get() = sharedPreferences.getString(KEY_RESPONSE_STYLE, "detailed") ?: "detailed"
        set(value) = sharedPreferences.edit().putString(KEY_RESPONSE_STYLE, value).apply()
}