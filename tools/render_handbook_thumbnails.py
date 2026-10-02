"""Render clean standing handbook icons from the mod's original rigs and textures.

Usage: python tools/render_handbook_thumbnails.py
Requires NumPy and Pillow only on the author's build machine. No image editing or outlines;
UV-textured geometry is rasterized with a depth buffer, using its idle pose at time zero.
"""
import ast
import json
import math
import os
from pathlib import Path
os.environ['OPENBLAS_NUM_THREADS'] = '1'
import numpy as np
from PIL import Image

SOURCE = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/guaniao'
OUT = SOURCE / 'textures/gui/handbook'


def rotation(angles, pivot):
    x, y, z = np.radians(angles)
    rx = np.array([[1, 0, 0], [0, np.cos(x), -np.sin(x)], [0, np.sin(x), np.cos(x)]])
    ry = np.array([[np.cos(y), 0, np.sin(y)], [0, 1, 0], [-np.sin(y), 0, np.cos(y)]])
    rz = np.array([[np.cos(z), -np.sin(z), 0], [np.sin(z), np.cos(z), 0], [0, 0, 1]])
    matrix = np.eye(4)
    matrix[:3, :3] = rz @ ry @ rx
    matrix[:3, 3] = np.array(pivot) - matrix[:3, :3] @ np.array(pivot)
    return matrix

def render(cubes, texture, texture_size, image_size=64, camera_angles=(25, -35, 0)):
    """Small orthographic inventory render with UV sampling and a depth buffer."""
    faces = {'east': [(1, 1, 1), (1, 1, 0), (1, 0, 0), (1, 0, 1)], 'west': [(0, 1, 0), (0, 1, 1), (0, 0, 1), (0, 0, 0)], 'up': [(0, 1, 0), (1, 1, 0), (1, 1, 1), (0, 1, 1)], 'down': [(0, 0, 1), (1, 0, 1), (1, 0, 0), (0, 0, 0)], 'south': [(0, 1, 1), (1, 1, 1), (1, 0, 1), (0, 0, 1)], 'north': [(1, 1, 0), (0, 1, 0), (0, 0, 0), (1, 0, 0)]}
    camera = rotation(camera_angles, [0, 0, 0])[:3, :3]
    quads = []
    for cube, transform in cubes:
        size = np.array(cube['size'], dtype=float)
        origin = np.array(cube['origin'], dtype=float)
        inflate = cube.get('inflate', 0)
        origin -= inflate
        size += inflate * 2
        uv = cube['uv']
        if isinstance(uv, list):
            u, v = uv
            w, h, d = cube['size']
            uv = {k: {'uv': p, 'uv_size': s} for k, p, s in [('west', [u, v + d], [d, h]), ('north', [u + d, v + d], [w, h]), ('east', [u + d + w, v + d], [d, h]), ('south', [u + 2 * d + w, v + d], [w, h]), ('up', [u + d, v], [w, d]), ('down', [u + d + w, v + d], [w, -d])]}
        for side, corners in faces.items():
            if side not in uv:
                continue
            points = origin + np.array(corners) * size
            points = (np.c_[points, np.ones(4)] @ transform.T)[:, :3] @ camera.T
            u, v = uv[side]['uv']
            w, h = uv[side]['uv_size']
            texcoords = np.array([[u, v], [u + w, v], [u + w, v + h], [u, v + h]], float)
            quads.append((points, texcoords, {'up': 1, 'down': 0.65, 'east': 0.8, 'west': 0.8, 'north': 0.9, 'south': 0.9}[side]))
    bounds = np.concatenate([q[0] for q in quads])
    low = bounds.min(0)
    high = bounds.max(0)
    scale = image_size * 0.875 / max((high - low)[:2])
    center = (low + high) / 2
    canvas = np.zeros((image_size, image_size, 4), dtype=np.uint8)
    depth = np.full((image_size, image_size), -np.inf)
    tex = np.array(texture.convert('RGBA'))
    th, tw = tex.shape[:2]
    for points, uv, shade in quads:
        points = (points - center) * scale
        points[:, 0] += image_size / 2
        points[:, 1] = image_size / 2 - points[:, 1]
        for ids in ([0, 1, 2], [0, 2, 3]):
            p = points[ids]
            t = uv[ids]
            lo = np.maximum(np.floor(p[:, :2].min(0)).astype(int), 0)
            hi = np.minimum(np.ceil(p[:, :2].max(0)).astype(int), image_size - 1)
            if np.any(hi < lo):
                continue
            yy, xx = np.mgrid[lo[1]:hi[1] + 1, lo[0]:hi[0] + 1]
            matrix = np.c_[p[:, :2], np.ones(3)].T
            if abs(np.linalg.det(matrix)) < 1e-08:
                continue
            bary = np.stack([xx + 0.5, yy + 0.5, np.ones_like(xx)], -1) @ np.linalg.inv(matrix).T
            z = bary @ p[:, 2]
            coords = bary @ t
            tx = np.clip((coords[..., 0] * tw / texture_size[0]).astype(int), 0, tw - 1)
            ty = np.clip((coords[..., 1] * th / texture_size[1]).astype(int), 0, th - 1)
            color = tex[ty, tx].copy()
            color[..., :3] = (color[..., :3] * shade).astype(np.uint8)
            mask = (bary.min(-1) >= -1e-06) & (z > depth[yy, xx]) & (color[..., 3] > 20)
            canvas[yy[mask], xx[mask]] = color[mask]
            depth[yy[mask], xx[mask]] = z[mask]
    return Image.fromarray(canvas)

def vector(value, default):
    if value is None:
        return np.array(default, dtype=float)
    while isinstance(value, dict):
        if 'vector' in value:
            value = value['vector']
        elif 'post' in value:
            value = value['post']
        elif 'pre' in value:
            value = value['pre']
        else:
            value = value[min(value, key=float)]

    def number(v):
        if isinstance(v, (int, float)):
            return v
        expression = v.replace('query.anim_time', '0').replace('math.', '')
        parsed = ast.parse(expression, mode='eval')
        allowed = (ast.Expression, ast.BinOp, ast.UnaryOp, ast.Constant, ast.Name, ast.Load, ast.Call, ast.Add, ast.Sub, ast.Mult, ast.Div, ast.USub, ast.UAdd)
        assert all((isinstance(n, allowed) for n in ast.walk(parsed))), expression
        return eval(compile(parsed, 'idle-expression', 'eval'), {'__builtins__': {}}, {'sin': lambda x: math.sin(math.radians(x)), 'cos': lambda x: math.cos(math.radians(x)), 'abs': abs, 'pow': pow, 'min': min, 'max': max})
    return np.array([number(v) for v in value], dtype=float)

def pos(v):
    return np.array(v) * [-1, 1, 1]

def rot(v):
    return rotation(np.array(v) * [-1, -1, 1], [0, 0, 0])

def translate(v):
    m = np.eye(4)
    m[:3, 3] = v
    return m

def render_all():
    pages=json.loads((SOURCE/'guide/zh_cn.json').read_text(encoding='utf-8'))['pages']
    textures={'cockatiel':'cockatiel/gray_yellow_face', 'macaw':'macaw/variant_1', 'pigeon':'pigeon_gray'}
    OUT.mkdir(parents=True,exist_ok=True)
    for page in pages:
        if page['kind']!='bird': continue
        name=page['model']
        rig='columbid' if name in ('spotted_dove','pigeon') else name
        geometry=json.loads((SOURCE/f'geo/{rig}.geo.json').read_text(encoding='utf-8'))['minecraft:geometry'][0]
        animations=json.loads((SOURCE/f'animations/{rig}.animation.json').read_text(encoding='utf-8'))['animations']
        idle=animations.get('idle',animations.get('animation.idle'))
        assert idle is not None,name
        bones={b['name']:b for b in geometry['bones']}; transforms={}
        def visit(key):
            if key in transforms: return transforms[key]
            b=bones[key]; parent=b.get('parent'); channels=idle.get('bones',{}).get(key,{})
            parent_pivot=pos(bones[parent].get('pivot',[0,0,0])) if parent else np.zeros(3)
            local=translate(pos(b.get('pivot',[0,0,0]))-parent_pivot+pos(vector(channels.get('position'),[0,0,0])))
            local=local@rot(np.array(b.get('rotation',[0,0,0]))+vector(channels.get('rotation'),[0,0,0]))@np.diag([*vector(channels.get('scale'),[1,1,1]),1])
            transforms[key]=(visit(parent) if parent else np.eye(4))@local
            return transforms[key]
        cubes=[]; view=rotation([0,math.degrees(2.35),0],[0,0,0])
        for key,b in bones.items():
            world=visit(key)
            if abs(np.linalg.det(world[:3,:3]))<1e-8: continue
            for c in b.get('cubes',[]):
                pivot=pos(c.get('pivot',b.get('pivot',[0,0,0])))
                size=np.array(c['size']); center=pos(np.array(c['origin'])+size/2)
                transform=view@world@translate(pivot-pos(b.get('pivot',[0,0,0])))@rot(c.get('rotation',[0,0,0]))@translate(center-pivot)
                cubes.append(({**c,'origin':list(-size/2)},transform))
        desc=geometry['description']
        with Image.open(SOURCE/f"textures/entity/{textures.get(name,name)}.png") as skin:
            poster=render(cubes,skin,[desc['texture_width'],desc['texture_height']],128,(0,0,0))
        assert poster.getbbox() is not None,name
        poster.save(OUT/f'{name}.png',optimize=True)
        print('Rendered standing model:',name)

if __name__ == '__main__':
    render_all()
