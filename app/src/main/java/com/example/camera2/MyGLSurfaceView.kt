package com.example.camera2

import android.content.Context
import android.opengl.GLES30
import android.view.SurfaceView

class MyGLSurfaceView(context: Context) : SurfaceView(context) {

    // GLThread

    inner class GLThread:Thread("GLThread") {

        override fun run() {
            while (true) {



            }
        }

    }

    // EGL

}