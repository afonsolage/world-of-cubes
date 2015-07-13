uniform vec4 g_LightColor;
uniform vec4 g_LightPosition;
uniform vec4 g_AmbientLightColor;

void main(){ 
    baseCoord = (tileSize * tileCoord);
    textCoord = textCoord * tileSize;

    lightingColor = g_LightColor.xyz * materialColor.xyz  + g_AmbientLightColor.xyz;

    projPosition = worldViewProjectionMatrix * vec4(vertexPosition, 1.0);
}