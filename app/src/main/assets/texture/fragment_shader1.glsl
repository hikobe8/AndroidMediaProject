precision mediump float;
uniform sampler2D vTexture;
varying vec2 aCoordinate;
void main(){
    gl_FragColor = vec4(vec3(1.0 - texture2D(vTexture, aCoordinate)), 1.0);
}
