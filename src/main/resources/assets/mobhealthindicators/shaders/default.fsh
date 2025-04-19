#version 150

in vec2 texCoord0;
uniform sampler2D Sampler0;
out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    if (color.a < 0.1) discard;
    fragColor = color;
}
