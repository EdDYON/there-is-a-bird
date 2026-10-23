#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform vec2 OutSize;
uniform float NearPlane;
uniform float FarPlane;
uniform float FocusDistance;
uniform float Aperture;
uniform float FocalLength;
uniform float DofMultiplier;
uniform float LensDistortion;

in vec2 texCoord;
out vec4 fragColor;

// Fixed disk samples avoid the square imprint of a sparse, enlarged 5x5 kernel.
const vec2 BLUR_DISK[32] = vec2[32](
    vec2(0.1250000, 0.0000000),
    vec2(-0.1596450, 0.1462479),
    vec2(0.0244362, -0.2784383),
    vec2(0.2012222, 0.2624588),
    vec2(-0.3692676, -0.0653182),
    vec2(0.3498025, -0.2225157),
    vec2(-0.1170021, 0.4352419),
    vec2(-0.2231357, -0.4296341),
    vec2(0.4841151, 0.1767981),
    vec2(-0.5036411, 0.2078957),
    vec2(0.2427883, -0.5188245),
    vec2(0.1794144, 0.5720013),
    vec2(-0.5407570, -0.3133797),
    vec2(0.6343695, -0.1394644),
    vec2(-0.3871458, 0.5506751),
    vec2(-0.0894397, -0.6901996),
    vec2(0.5490718, 0.4627583),
    vec2(-0.7388785, 0.0305549),
    vec2(0.5389551, -0.5363323),
    vec2(-0.0360582, 0.7797915),
    vec2(-0.5128175, -0.6145268),
    vec2(0.8123596, 0.1093018),
    vec2(-0.6883106, 0.4789086),
    vec2(0.1880861, -0.8360614),
    vec2(0.4350333, 0.7591911),
    vec2(-0.8504484, -0.2713162),
    vec2(0.8261024, -0.3816803),
    vec2(-0.3578882, 0.8551556),
    vec2(-0.3194073, -0.8880338),
    vec2(0.8499086, 0.4466882),
    vec2(-0.9440346, 0.2488445),
    vec2(0.5365958, -0.8345298)
);

float linearDepth(float depth) {
    float z = depth * 2.0 - 1.0;
    return (2.0 * NearPlane * FarPlane)
        / max(0.0001, FarPlane + NearPlane - z * (FarPlane - NearPlane));
}

vec2 lensUv(vec2 uv) {
    vec2 centered = uv * 2.0 - 1.0;
    float aspect = OutSize.x / max(OutSize.y, 1.0);
    vec2 optical = vec2(centered.x * aspect, centered.y);
    float radiusSquared = dot(optical, optical);
    optical *= 1.0 - LensDistortion * min(radiusSquared, 2.5) * 0.22;
    centered = vec2(optical.x / max(aspect, 0.0001), optical.y);
    return clamp(centered * 0.5 + 0.5, vec2(0.001), vec2(0.999));
}

// Signed thin-lens CoC in output pixels: foreground is negative, background positive.
float circleOfConfusion(float sceneDistance, bool sky) {
    bool infiniteFocus = FocusDistance >= 127.5;
    float inverseFocus = infiniteFocus ? 0.0 : 1.0 / max(FocusDistance, 0.3);
    float inverseScene = sky ? 0.0 : 1.0 / max(sceneDistance, NearPlane);
    float focalMeters = FocalLength * 0.001;
    float sensorCoc = focalMeters * focalMeters * (inverseFocus - inverseScene)
        / (max(Aperture, 1.4) * max(1.0 - focalMeters * inverseFocus, 0.05));
    float focusFade = infiniteFocus || sky ? 1.0
        : smoothstep(0.02, 0.05, abs(sceneDistance - FocusDistance) / max(FocusDistance, 0.3));
    // A modest photographic exaggeration remains consistent when the window is resized.
    return sensorCoc * max(OutSize.y, 1.0) / (2.0 * 0.024)
        * 1.8 * DofMultiplier * focusFade;
}

void main() {
    vec2 uv = lensUv(texCoord);
    vec3 center = texture(DiffuseSampler, uv).rgb;
    if (DofMultiplier <= 0.0) {
        fragColor = vec4(center, 1.0);
        return;
    }
    float depth = texture(DepthSampler, uv).r;
    float sceneDistance = linearDepth(depth);
    float coc = circleOfConfusion(sceneDistance, depth >= 0.999999);
    float resolutionScale = max(OutSize.y, 1.0) / 1080.0;
    float maximumRadius = 24.0 * resolutionScale;
    // A soft limit preserves the difference between wide apertures on a long lens.
    float radius = maximumRadius * (1.0 - exp(-abs(coc) / maximumRadius));
    if (radius < 0.3 * resolutionScale) {
        fragColor = vec4(center, 1.0);
        return;
    }

    vec2 texelRadius = radius / max(OutSize, vec2(1.0));
    vec3 color = center * 0.5;
    float totalWeight = 0.5;
    for (int index = 0; index < 32; ++index) {
        vec2 offset = BLUR_DISK[index];
        vec2 sampleUv = lensUv(texCoord + offset * texelRadius);
        float sampleDepth = texture(DepthSampler, sampleUv).r;
        float sampleCoc = circleOfConfusion(linearDepth(sampleDepth), sampleDepth >= 0.999999);

        // Blurred backgrounds may mix across distant surfaces and sky, but never
        // gather an in-focus bird or an object on the other side of the focal plane.
        if (coc * sampleCoc <= 0.0) {
            continue;
        }
        float layerWeight = smoothstep(0.10, 0.50, abs(sampleCoc) / max(abs(coc), 0.0001));
        float weight = exp(-1.5 * dot(offset, offset)) * layerWeight;
        color += texture(DiffuseSampler, sampleUv).rgb * weight;
        totalWeight += weight;
    }

    // Rejected samples reduce only coverage; they must never turn an edge dark.
    fragColor = vec4(color / totalWeight, 1.0);
}
