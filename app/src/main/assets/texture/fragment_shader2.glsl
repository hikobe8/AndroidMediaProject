precision mediump float;
varying vec2 aCoordinate;
uniform sampler2D vTexture;
void main() {
    lowp vec4 textureColor = texture2D(vTexture, aCoordinate);
     float gray = textureColor.r * 0.2125 + textureColor.g * 0.7154 + textureColor.b * 0.0721;
     gl_FragColor = vec4(gray, gray, gray, textureColor.w);
}