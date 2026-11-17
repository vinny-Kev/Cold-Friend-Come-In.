#import "Common/ShaderLib/GLSLCompat.glsllib"

// Water surface vertex shader. Per-vertex data comes from WaterMeshBuilder:
//   inTexCoord = ripple pattern coordinates in metres (x across, y along the flow; lakes use world x/z)
//   inColor    = (shoreline fade 0..1, depth 0..1, flow speed / 3 m/s, frozen 0 or 1)

uniform mat4 g_WorldViewProjectionMatrix;
uniform mat4 g_WorldMatrix;

attribute vec3 inPosition;
attribute vec2 inTexCoord;
attribute vec4 inColor;

varying vec3 vWorld;
varying vec2 vPattern;
varying vec4 vInfo;

void main() {
    vWorld = (g_WorldMatrix * vec4(inPosition, 1.0)).xyz;
    vPattern = inTexCoord;
    vInfo = inColor;
    gl_Position = g_WorldViewProjectionMatrix * vec4(inPosition, 1.0);
}
