package com.example.camera2

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.*
import android.media.*
import android.media.MediaCodec.createPersistentInputSurface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.camera2.databinding.ActivityMainBinding
import java.io.File
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater).apply {
            setContentView(this.root)

            mainCamera.setOnClickListener {
                startActivity(Intent(this@MainActivity, CameraActivity::class.java))
            }

            mainRecordVideo.setOnClickListener {
                startActivity(Intent(this@MainActivity, RecordActivity::class.java))
            }
        }

    }

}

