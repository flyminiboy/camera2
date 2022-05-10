package com.example.camera2

import android.opengl.GLES11Ext
import android.opengl.GLES30
import java.lang.RuntimeException
import javax.microedition.khronos.opengles.GL11Ext

object TextureUtil {

    /**
     * 通过 OpenGL 构建一个纹理，并返回纹理ID
     *
     * type 纹理类型，2d / oes扩展纹理
     */
    fun createTexture(type:Int):Int {

        val textures = IntArray(1)
        GLES30.glGenTextures(textures.size, textures, 0)

        val textureTarget = if (type == 0) {
            GLES30.GL_TEXTURE_2D
        } else if (type == 1){
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES
        } else {
            throw RuntimeException("error type ${type},most be 0 or 1")
        }

        GLES30.glTexParameteri(
            textureTarget,
            GLES30.GL_TEXTURE_WRAP_S,
            GLES30.GL_REPEAT
        )
        GLES30.glTexParameteri(
            textureTarget,
            GLES30.GL_TEXTURE_WRAP_T,
            GLES30.GL_REPEAT
        )
        GLES30.glTexParameteri(
            textureTarget,
            GLES30.GL_TEXTURE_MIN_FILTER,
            GLES30.GL_LINEAR
        )
        GLES30.glTexParameteri(
            textureTarget,
            GLES30.GL_TEXTURE_MAG_FILTER,
            GLES30.GL_LINEAR
        )

        GLES30.glBindTexture(textureTarget, textures[0])
        return textures[0]

    }

    fun activeateTeture(textureId:Int) {

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0) // 激活纹理单元
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId) // 绑定纹理到对应的纹理单元

    // 不管 sampler2D 还是 samplerExternalOES 变量都是 uniform ，为什么没有使用 glUniform1i 赋值？
        // 使用 glUniform1i 我们可以给纹理采样器分配一个位置值，这样我们可以在一个片段着色器中设置多个纹理。
        // 一个纹理位置通常称为一个 纹理单元，一个纹理的默认纹理单元是0，他是默认的激活纹理单元。
        // GLES30.glUniform1i() 注意参数
        // 第一个参数 位置
        // 第二个参数 对应的纹理单元

    }

}