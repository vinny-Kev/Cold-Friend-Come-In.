#import "Common/ShaderLib/GLSLCompat.glsllib"

// Wind sway for foliage. Per-vertex data comes from BushMeshBuilder:
//   inTexCoord = (sway weight 0..1: 0 at the roots, growing toward the tips, leaf phase 0..1)
//   inColor    = base colour
// The wind is a wave travelling along the wind direction (so a gust visibly rolls across the bushes) plus a
// small fast flutter per leaf cluster. Everything is a function of world position and engine time.

uniform mat4 g_ViewProjectionMatrix;
uniform mat4 g_WorldMatrix;
uniform float g_Time;

uniform vec3 m_WindDirection;
uniform float m_WindStrength;
uniform float m_GustStrength;

attribute vec3 inPosition;
attribute vec3 inNormal;
attribute vec2 inTexCoord;
attribute vec4 inColor;

varying vec3 vNormal;
varying vec4 vColor;

const float TWO_PI = 6.2831853;
const float MAX_SWAY = 0.35;     // metres of tip movement at strength 1
const float DROOP = 0.4;         // tips dip as they bend, so branches do not stretch

void main() {
    vec4 world = g_WorldMatrix * vec4(inPosition, 1.0);
    float weight = inTexCoord.x;
    float phase = inTexCoord.y * TWO_PI;

    float along = dot(world.xz, m_WindDirection.xz);
    float wave = sin(g_Time * 1.3 - along * 0.18);
    float gustEnvelope = 0.5 + 0.5 * sin(g_Time * 0.35 - along * 0.05);
    float push = m_WindStrength * (0.6 + 0.4 * wave) + m_GustStrength * gustEnvelope * (0.5 + 0.5 * wave);
    float flutter = sin(g_Time * 7.0 + phase) * 0.12 * (m_WindStrength + m_GustStrength);

    vec2 side = vec2(-m_WindDirection.z, m_WindDirection.x);
    vec2 offset = (m_WindDirection.xz * push + side * flutter) * MAX_SWAY * weight;
    world.xz += offset;
    world.y -= length(offset) * DROOP;

    vNormal = mat3(g_WorldMatrix) * inNormal;
    vColor = inColor;
    gl_Position = g_ViewProjectionMatrix * world;
}
