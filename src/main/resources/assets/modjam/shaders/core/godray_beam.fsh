#version 330

layout(std140) uniform Globals {
    ivec3 CameraBlockPos;
    vec3 CameraOffset;
    vec2 ScreenSize;
    float GlintAlpha;
    float GameTime;
    int MenuBlurRadius;
    int UseRgss;
};

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

// texCoord0.x = distance from the beam axis, 0.0 at the center .. 1.0 at the plane edge.
// texCoord0.y = vertical scroll coordinate along the beam (repeats).

const float CORE_TIGHTNESS = 22.0;   // how narrow the hot center column is
const float BODY_TIGHTNESS = 4.5;    // how wide the soft outer glow falls away
const float BAND_COUNT = 7.0;        // descending light discs per texture tile
const float BAND_SPEED = 0.7;        // how fast they fall
const float BAND_TIGHTNESS = 5.0;    // radial tightness of the discs (higher = narrower)
const float SHIMMER_AMOUNT = 0.12;   // gentle per-pixel flicker near the core

void main() {
    float radial = clamp(texCoord0.x, 0.0, 1.0);
    float time = GameTime;
    float v = fract(texCoord0.y);

    // Hot thin center column and a broad translucent haze around it.
    float core = pow(1.0 - radial, CORE_TIGHTNESS);
    float body = pow(1.0 - radial, BODY_TIGHTNESS);
    float base = core * 1.6 + body * 0.85;

    // Light discs that fall continuously toward the ground.
    float bounce = v * BAND_COUNT - time * BAND_SPEED;
    float ring = 0.5 + 0.5 * sin(bounce * 6.2831853);
    float bandBoost = pow(ring, BAND_TIGHTNESS) * smoothstep(0.0, 0.55, 1.0 - v);

    // Slight shimmer flicker so the shaft feels alive, most visible near the core.
    float shimmer = 1.0 + SHIMMER_AMOUNT * sin(radial * 9.0 - time * 6.0);

    // Compose: alpha carries intensity so additive SRC_ALPHA/ONE blending glows.
    float energy = (base + bandBoost) * shimmer;
    vec3 color = vertexColor.rgb * energy;
    fragColor = vec4(color, energy);
}