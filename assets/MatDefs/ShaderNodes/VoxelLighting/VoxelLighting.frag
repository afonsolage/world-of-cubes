void main(){
    textCoord = mod(textCoord, tileSize);
    outColor = texture2D(textureMap, baseCoord+textCoord) * vec4(lightingColor, 1.0);
}