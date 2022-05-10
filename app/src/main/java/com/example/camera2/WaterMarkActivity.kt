package com.example.camera2

import android.opengl.GLSurfaceView
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.camera2.databinding.ActivityMainBinding
import com.example.camera2.databinding.ActivityWaterMarkBinding

class WaterMarkActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWaterMarkBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWaterMarkBinding.inflate(layoutInflater).apply {


            val surfaceView = GLSurfaceView(this@WaterMarkActivity)
            surfaceView.setEGLContextClientVersion(3)
            surfaceView.setRenderer(PlayRender(VideoDrawer()))
            surfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
            root.addView(surfaceView)
            setContentView(root)
        }
    }
}