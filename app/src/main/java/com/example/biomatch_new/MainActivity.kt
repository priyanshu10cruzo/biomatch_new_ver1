package com.example.biomatch_new

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.graphics.drawable.BitmapDrawable
import android.media.Image
import android.media.ImageReader
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.YOLOIntegration.YOLOv8Analyzer
import kotlinx.coroutines.yield
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.tensorflow.lite.Interpreter
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

var hasTAkenPhoto = false
var i = 0

class MainActivity : AppCompatActivity() {

    lateinit var cameraExecutor: ExecutorService
    lateinit var previewView: PreviewView
    lateinit var overlayBox: View
    lateinit var imageCapture: ImageCapture
    lateinit var cameraControl: CameraControl
    lateinit var tflite: Interpreter
    var isFlashOn = false
    var brightnessThreshold = 100.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tflite = Interpreter(loadModelFile("best_float32.tflite"))

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        previewView = findViewById(R.id.previewView)
        overlayBox = findViewById(R.id.overlay_box)
//        val captureButton: Button = findViewById(R.id.capture_button)
        val flashToggleButton: ImageButton = findViewById(R.id.flashImageView)
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

//        captureButton.setOnClickListener {
//            capturePhoto()
//        }

        flashToggleButton.setOnClickListener {
            isFlashOn = !isFlashOn
            imageCapture.flashMode = if (isFlashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
        }

        previewView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val factory = previewView.meteringPointFactory
                val point = factory.createPoint(event.x, event.y)
                val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF).build()
                cameraControl.startFocusAndMetering(action)
                true
            } else {
                false
            }
        }
    }

    fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setFlashMode(if (isFlashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF)
                .build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
                        if (luma < brightnessThreshold) {
                            runOnUiThread {
                                imageCapture.flashMode = ImageCapture.FLASH_MODE_ON
                                capturePhoto()
//                                onDestroy()
                            }
                        } else {
                            runOnUiThread {
                                imageCapture.flashMode = if (isFlashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
                                capturePhoto()
//                                onDestroy()
                            }
                        }
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA


            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer
                )
                cameraControl = camera.cameraControl
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    fun capturePhoto() {
        val photoFile = createFile(
            baseContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            FILENAME,
            PHOTO_EXTENSION
        )

//        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback(){
                override fun onCaptureSuccess(image: ImageProxy) {
                    i++
                    processImage(image)
//                    onDestroy()
                    Log.d(TAG,"value of counter ${i}")
                }
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

//                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
//                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
//                    val croppedBitmap = cropBitmap(bitmap)
//                    val processedBitmapp = preprocessImagee(croppedBitmap)
//                    if (processedBitmapp == 12)
//                    {
//                        Toast.makeText(this@MainActivity,"Image not upto mark,Retake!",Toast.LENGTH_LONG).show()
//                    }
//                    else
//                    {val processedBitmap = preprocessImage(croppedBitmap)
////                    println(processedBitmapp)
//                    runOnUiThread {
//                        overlayBox.background = BitmapDrawable(resources, processedBitmap)
//                    }
//                }}
            })
    }
    fun processImage(image: ImageProxy){
        val yoloAnalyzer = YOLOv8Analyzer(tflite) { processedBitmap, results ->
            val pBitmap = preprocessImagee(processedBitmap)
            if (pBitmap == 12)
                    {
                        Toast.makeText(this@MainActivity,"Image not upto mark,Retake!",Toast.LENGTH_LONG).show()
                    }
                    else
                    {val preprocessedBitmap = preprocessImage(processedBitmap)
                        saveProcessedImage(preprocessedBitmap)
                        saveProcessedImage(processedBitmap)
                        val bitmap: Bitmap = preprocessedBitmap
                        val stream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.PNG,100,stream)
                        val byteArray = stream.toByteArray()
                        val intent = Intent(this ,MainActivity3::class.java)
                        intent.putExtra("bitmap",byteArray)
                        startActivity( intent)
//                        hasTAkenPhoto = true
//                        onDestroy()
//                        Log.d(TAG, "processedBitmapp $preprocessedBitmap")
//                    runOnUiThread {
//                        overlayBox.background = BitmapDrawable(resources, preprocessedBitmap)
//                    }
                }
        }
        yoloAnalyzer.analyze(image)
    }

    fun saveProcessedImage(bitmap: Bitmap) {
        val filename = "processed_image_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/YOLOApp")
            }
        }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            contentResolver.openOutputStream(it)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
            Log.d(TAG, "Image saved successfully: $filename")
            runOnUiThread {
                Toast.makeText(this, "Processed image saved: $filename", Toast.LENGTH_SHORT).show()
            }
        }?: Log.e(TAG, "Failed to create new MediaStore record.")
    }

    fun cropBitmap(bitmap: Bitmap): Bitmap {
        val box = overlayBox
        val scaleX = bitmap.width.toFloat() / previewView.width.toFloat()
        val scaleY = bitmap.height.toFloat() / previewView.height.toFloat()

        val left = (box.left * scaleX).toInt()
        val top = (box.top * scaleY).toInt()
        val width = (box.width * scaleX).toInt()
        val height = (box.height * scaleY).toInt()

        val safeLeft = left.coerceAtLeast(0)
        val safeTop = top.coerceAtLeast(0)
        val safeWidth = if (safeLeft + width > bitmap.width) bitmap.width - safeLeft else width
        val safeHeight = if (safeTop + height > bitmap.height) bitmap.height - safeTop else height

        return Bitmap.createBitmap(bitmap, safeLeft, safeTop, safeWidth, safeHeight)
    }

    fun createFile(baseFolder: File?, format: String, extension: String): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "${format}_${timeStamp}.$extension"
        return File(baseFolder, imageFileName)
    }

    fun getStringImage(bitmap: Bitmap?): String? {
        val baos = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.PNG, 100, baos)
        val imgByte = baos.toByteArray()
        return Base64.encodeToString(imgByte, Base64.DEFAULT)
    }
    fun preprocessImagee(croppedBitmap: Bitmap?): Int {
        val py = Python.getInstance()
        val pyObj = py.getModule("myscript2")
        val imageStr = getStringImage(croppedBitmap)
        val obj = pyObj.callAttr("main", imageStr)
        val objj = obj.toInt()
        return objj
//        val imgStr = obj.toString()
//        val data = Base64.decode(imgStr, Base64.DEFAULT)
//        return BitmapFactory.decodeByteArray(data, 0, data.size)
    }

    fun loadModelFile(filename: String): MappedByteBuffer {
        val fileDescriptor = assets.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }


    fun preprocessImage(croppedBitmap: Bitmap?): Bitmap {
        val py = Python.getInstance()
        val pyObj = py.getModule("myscript")
        val imageStr = getStringImage(croppedBitmap)
        val obj = pyObj.callAttr("main", imageStr, 11)
        val imgStr = obj.toString()
        val data = Base64.decode(imgStr, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(data, 0, data.size)
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val FILENAME = "captured_image"
        private const val PHOTO_EXTENSION = "jpg"
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    class LuminosityAnalyzer(private val listener: (Double) -> Unit) : ImageAnalysis.Analyzer {
        override fun analyze(image: ImageProxy) {
            val buffer = image.planes[0].buffer
            val data = buffer.toByteArray()
            val pixels = data.map { it.toInt() and 0xFF }
            val luma = pixels.average()
            listener(luma)
            image.close()
        }

        fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }
    }
}
