"""Measure a Bedrock geo.json model's authored standing height in model pixels.

Method: rest-pose Y span. Cube origins are already in model space (a bone's
pivot only defines its rotation centre), so only rotations are propagated down
the bone chain; pivots are NOT added to cube origins.

Calibration against the values already recorded in BirdModelScaleProfile:
three species reproduce exactly (sparrow 6.120, cockatiel 17.628,
cassowary 29.424). The remaining recorded values were hand-noted when the
models had slightly different proportions and have since drifted by <1.6 px,
so freshly authored models are measured with this script rather than copied.
"""
from pathlib import Path
import json
import math
import os
import sys


def mat_identity():
    return [[1.0, 0.0, 0.0, 0.0], [0.0, 1.0, 0.0, 0.0], [0.0, 0.0, 1.0, 0.0], [0.0, 0.0, 0.0, 1.0]]


def mat_mul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(4)) for j in range(4)] for i in range(4)]


def mat_translate(x, y, z):
    m = mat_identity()
    m[0][3], m[1][3], m[2][3] = x, y, z
    return m


def mat_rot_x(deg):
    r = math.radians(deg)
    c, s = math.cos(r), math.sin(r)
    m = mat_identity()
    m[1][1], m[1][2], m[2][1], m[2][2] = c, -s, s, c
    return m


def mat_rot_y(deg):
    r = math.radians(deg)
    c, s = math.cos(r), math.sin(r)
    m = mat_identity()
    m[0][0], m[0][2], m[2][0], m[2][2] = c, s, -s, c
    return m


def mat_rot_z(deg):
    r = math.radians(deg)
    c, s = math.cos(r), math.sin(r)
    m = mat_identity()
    m[0][0], m[0][1], m[1][0], m[1][1] = c, -s, s, c
    return m


def mat_apply(m, p):
    x, y, z = p
    return (
        m[0][0] * x + m[0][1] * y + m[0][2] * z + m[0][3],
        m[1][0] * x + m[1][1] * y + m[1][2] * z + m[1][3],
        m[2][0] * x + m[2][1] * y + m[2][2] * z + m[2][3],
    )


def measure(path):
    """Return (min_y, max_y, per_bone) for the model's rest pose."""
    data = json.load(open(path, encoding="utf-8"))
    geometry = data["minecraft:geometry"][0]
    bones = geometry.get("bones", [])
    per_bone = {}

    def walk(bone, parent_matrix):
        pivot = bone.get("pivot", [0.0, 0.0, 0.0])
        rot = bone.get("rotation", [0.0, 0.0, 0.0])
        local = mat_translate(pivot[0], pivot[1], pivot[2])
        local = mat_mul(local, mat_rot_z(rot[2]))
        local = mat_mul(local, mat_rot_y(rot[1]))
        local = mat_mul(local, mat_rot_x(rot[0]))
        local = mat_mul(local, mat_translate(-pivot[0], -pivot[1], -pivot[2]))
        world = mat_mul(parent_matrix, local)

        ys = []
        for cube in bone.get("cubes", []):
            origin = cube["origin"]
            size = cube["size"]
            lo = list(origin)
            hi = [origin[i] + size[i] for i in range(3)]
            for dx in (lo[0], hi[0]):
                for dy in (lo[1], hi[1]):
                    for dz in (lo[2], hi[2]):
                        ys.append(mat_apply(world, (dx, dy, dz))[1])
        if ys:
            per_bone[bone["name"]] = (min(ys), max(ys))
        for child in bones:
            if child.get("parent") == bone["name"]:
                walk(child, world)

    for root in bones:
        if not root.get("parent"):
            walk(root, mat_identity())

    all_y = [v for v in per_bone.values()]
    return min(v[0] for v in all_y), max(v[1] for v in all_y), per_bone


KNOWN = {
    "night_heron": 17.251,
    "sparrow": 6.120,
    "long_tailed_tit": 7.663,
    "budgerigar": 13.572,
    "cockatiel": 17.628,
    "macaw": 20.796,
    "crow": 16.267,
    "seagull": 16.935,
    "kiwi": 17.400,
    "myna": 11.969,
    "kestrel": 9.980,
    "cassowary": 29.424,
}


if __name__ == "__main__":
    root = str(Path(__file__).resolve().parent / "src/main/resources/assets/guaniao/geo") + os.sep
    if len(sys.argv) > 1:
        for name in sys.argv[1:]:
            lo, hi, _ = measure(root + name + ".geo.json")
            print(f"{name}: span={hi - lo:.4f}  (min {lo:.4f} max {hi:.4f})")
    else:
        print(f"{'species':<18}{'recorded':>10}{'measured':>10}{'delta':>9}")
        for name, known in KNOWN.items():
            path = root + name + ".geo.json"
            if not os.path.exists(path):
                print(f"{name:<18}{known:>10.3f}{'missing':>10}")
                continue
            lo, hi, _ = measure(path)
            span = hi - lo
            print(f"{name:<18}{known:>10.3f}{span:>10.3f}{span - known:>9.3f}")
