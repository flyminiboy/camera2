#version 300 es

layout (location = 0) in vec4 vPosition;
layout (location = 1) in vec2 tPosition;
//layout (location = 1) in vec4 tPosition;

uniform mat4 uTextureMatrix;

out vec2 outTexturePosition;

void main() {
    gl_Position = vPosition; // gl_Position 内建变量，是一个vec4类型
//    outTexturePosition = (tPosition * uTextureMatrix).xy;
    outTexturePosition = tPosition;
}
