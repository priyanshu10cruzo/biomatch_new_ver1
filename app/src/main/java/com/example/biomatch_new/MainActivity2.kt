package com.example.biomatch_new

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
//import com.example.biomatch_new.MainActivity.Companion.REQUIRED_PERMISSIONS

class MainActivity2 : AppCompatActivity() {

    private lateinit var permissionHandler: PermissionHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main2)
        permissionHandler = PermissionHandler(this)

        if (permissionHandler.arePermissionsGranted(REQUIRED_PERMISSIONS)) {
            val button: Button = findViewById(R.id.authenticateButton)

            button.setOnClickListener{
                val intent = Intent(this ,MainActivity::class.java)
                startActivity( intent)
            }
        }
        else {
            permissionHandler.requestPermissions(REQUIRED_PERMISSIONS, PermissionHandler.REQUEST_CODE_PERMISSIONS)
        }

//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        permissionHandler.handleRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults,
            onGranted = {
                val button: Button = findViewById(R.id.authenticateButton)

                button.setOnClickListener{
                    val intent = Intent(this ,MainActivity::class.java)
                    startActivity( intent)
                }
            },
            onDenied = {
                // Permission denied, show a message to the user
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val REQUEST_CODE_PERMISSIONS = 10
        val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val FILENAME = "captured_image"
        private const val PHOTO_EXTENSION = "jpg"
    }
}
