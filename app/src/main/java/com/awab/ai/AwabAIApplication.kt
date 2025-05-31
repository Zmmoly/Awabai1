package com.awab.ai

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.awab.ai.data.database.AppDatabase
import com.awab.ai.ml.ModelManager
import com.awab.ai.utils.PreferencesManager

class AwabAIApplication : Application() {
    
    companion object {
        lateinit var instance: AwabAIApplication
            private set
    }
    
    val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "awab_ai_database"
        ).fallbackToDestructiveMigration().build()
    }
    
    val preferencesManager by lazy {
        PreferencesManager(applicationContext)
    }
    
    val modelManager by lazy {
        ModelManager(applicationContext)
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // تهيئة المكونات الأساسية
        initializeComponents()
    }
    
    private fun initializeComponents() {
        // تهيئة النماذج في الخلفية
        Thread {
            modelManager.initializeModels()
        }.start()
    }
    
    fun getAppContext(): Context = applicationContext
}