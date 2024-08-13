package com.example.biomatch_new

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.chaquo.python.Python
import kotlinx.coroutines.CoroutineStart
import java.io.ByteArrayOutputStream
//import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import android.util.Base64
import androidx.camera.core.*
import java.io.File

var j =0
class MainActivity3 : AppCompatActivity() {
    @OptIn(ExperimentalEncodingApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main3)

        val byteArray = intent.getByteArrayExtra("bitmap")
        val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray?.size ?: 0)
        j++
        val imageView: ImageView = findViewById(R.id.imageView)
        if (j == 1) {
            imageView.setImageBitmap(bitmap)
            val pyy = Python.getInstance()
            val pyObj = pyy.getModule("myscript3")
            val imageStr = getStringImage(bitmap)
            val context = applicationContext
            val directory = File(context.filesDir, "iso_files")
            if (!directory.exists()) {
                directory.mkdir()
            }
            val filepath = directory.absolutePath
            pyObj.callAttr("main", imageStr, filepath)
        }
    } fun getStringImage(bitmap: Bitmap?): String? {
           val baos = ByteArrayOutputStream()
           bitmap?.compress(Bitmap.CompressFormat.PNG, 100, baos)
           val imgByte = baos.toByteArray()
           return Base64.encodeToString(imgByte, Base64.DEFAULT)
       }}