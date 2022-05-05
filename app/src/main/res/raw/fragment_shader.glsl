#version 300 es
#extension GL_OES_EGL_image_external:require
precision mediump float;

in vec2 outTexturePosition;

uniform samplerExternalOES sTexture; // 扩展纹理

out vec4 outColor;

void main() {
    outColor = texture(sTexture, outTexturePosition); // texture函数会使用之前设置的纹理参数对相应的颜色值进行采样。这个片段着色器的输出就是纹理的（插值）纹理坐标上的(过滤后的)颜色
}
