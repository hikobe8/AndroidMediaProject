precision mediump float;
varying vec2 aCoordinate;
uniform sampler2D vTexture;
void main() {
    lowp vec4 textureColor = texture2D(vTexture, aCoordinate);
    gl_FragColor = vec4((textureColor.rgb + vec3(-0.5f)), textureColor.w);
}
