#import "Common/ShaderLib/GLSLCompat.glsllib"

// Water surface fragment shader: animated ripple normals, depth tint, sky reflection, sun glint, shoreline
// fade, and a still cracked-ice look when the vertex data says the body is frozen. No textures, no extra passes.

uniform float g_Time;
uniform vec3 g_CameraPosition;

uniform vec4 m_ShallowColor;
uniform vec4 m_DeepColor;
uniform vec4 m_IceColor;
uniform vec4 m_SkyColor;
uniform vec4 m_SunColor;
uniform vec4 m_AmbientColor;
uniform vec3 m_SunDirection;

varying vec3 vWorld;
varying vec2 vPattern;
varying vec4 vInfo;

const float MAX_FLOW = 3.0;

// Derivative of a sum of three travelling sine waves with respect to the pattern coordinates.
vec2 rippleSlope(vec2 p, float t) {
    vec2 k1 = vec2(1.3, 0.7);
    vec2 k2 = vec2(-0.6, 1.9);
    vec2 k3 = vec2(2.7, -1.1);
    return 0.050 * k1 * cos(dot(p, k1) + t * 1.1)
         + 0.035 * k2 * cos(dot(p, k2) - t * 0.8)
         + 0.020 * k3 * cos(dot(p, k3) + t * 1.7);
}

void main() {
    float shore = vInfo.r;
    float depth = vInfo.g;
    float flow = vInfo.b * MAX_FLOW;
    bool frozen = vInfo.a > 0.5;

    vec2 p = vPattern;
    p.y -= g_Time * flow;

    vec3 n = vec3(0.0, 1.0, 0.0);
    if (!frozen) {
        vec2 slope = rippleSlope(p, g_Time) * (1.0 + flow);
        n = normalize(vec3(-slope.x, 1.0, -slope.y));
    }
    vec3 v = normalize(g_CameraPosition - vWorld);

    vec3 base;
    float alpha;
    if (frozen) {
        // Faint crack lines over pale ice; opaque enough to hide the bed.
        float crack = 1.0 - smoothstep(0.0, 0.06, abs(sin(p.x * 0.8 + 1.7 * sin(p.y * 0.55)) * sin(p.y * 0.45 + p.x * 0.3)));
        base = m_IceColor.rgb * (1.0 - 0.18 * crack) * (1.0 - 0.15 * depth);
        alpha = 0.93;
    } else {
        base = mix(m_ShallowColor.rgb, m_DeepColor.rgb, clamp(depth, 0.0, 1.0));
        alpha = mix(0.55, 0.92, clamp(depth, 0.0, 1.0));
    }

    float diffuse = max(dot(n, -normalize(m_SunDirection)), 0.0);
    vec3 lit = base * (m_AmbientColor.rgb + m_SunColor.rgb * diffuse * 0.5);

    float fresnel = pow(1.0 - max(dot(n, v), 0.0), 4.0) * (frozen ? 0.25 : 0.8);
    lit = mix(lit, m_SkyColor.rgb, fresnel);

    float glint = pow(max(dot(reflect(normalize(m_SunDirection), n), v), 0.0), frozen ? 24.0 : 90.0);
    lit += m_SunColor.rgb * glint * (frozen ? 0.15 : 0.6);

    // Shoreline: fade the edge into the bank and lay a pale foam/snow line along it.
    float foam = (1.0 - smoothstep(0.0, 0.5, shore)) * (frozen ? 0.25 : 0.35);
    lit = mix(lit, m_AmbientColor.rgb + m_SunColor.rgb * 0.3, foam);
    alpha *= smoothstep(0.0, 0.3, shore);

    gl_FragColor = vec4(lit, alpha);
}
