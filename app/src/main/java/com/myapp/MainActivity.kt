package com.myapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.myapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // هنا يمكنك إضافة الكود الخاص بك
        binding.textView.text = "مرحباً بك في التطبيق!"
    }
}