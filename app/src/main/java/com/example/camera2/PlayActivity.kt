package com.example.camera2

import android.opengl.GLSurfaceView
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.camera2.databinding.ActivityPlayBinding

class PlayActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayBinding.inflate(layoutInflater).apply {


            val surfaceView = GLSurfaceView(this@PlayActivity)
            surfaceView.setEGLContextClientVersion(3)
//            surfaceView.setRenderer(PlayRender(VideoDrawer()))
            surfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
            root.addView(surfaceView)
            setContentView(root)
        }
    }
}