package com.example.biomatch_new

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
var j =0
class MainActivity3 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main3)

        val byteArray = intent.getByteArrayExtra("bitmap")
        val bitmap = BitmapFactory.decodeByteArray(byteArray,0,byteArray?.size ?: 0)
        j++
        val imageView: ImageView = findViewById(R.id.imageView)
        if(j == 2){
        imageView.setImageBitmap(bitmap)}
    }}