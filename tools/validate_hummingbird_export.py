"""Compare the hummingbird export with the author's rig and all six clips.

This checker evaluates the source with the THREE.SplineCurve Hermite basis,
independently of the exporter's expanded polynomial. It writes numerical
references for the Java test that loads the real GeckoLib 4.4.9 baker.
"""
import argparse
import ast
import hashlib
import json
import math
import re
from pathlib import Path
from types import SimpleNamespace

ROOT = Path(__file__).resolve().parents[1]
SOURCE_DIR = Path('C:/Users/25773/Desktop/模型/蜂鸟')
SOURCE_FILES = {'animation':'model.animation.json', 'geometry':'蜂鸟26.9.26Anim.geo.json', 'texture':'texture.png'}
ASSETS = ROOT / 'src/main/resources/assets/guaniao'
OUT = ROOT / 'build/verification/hummingbird-animation-review'
BODY = {'root', 'all', 'head', 'waist', 'hip', 'body', 'tail', 'Wing',
        'leg_left', 'leg_right', 'foot_left', 'foot_right'}
WINGS = {'wing_left', 'wing_right'}
DEFAULTS = {'rotation': [0, 0, 0], 'position': [0, 0, 0], 'scale': [1, 1, 1]}


def evaluate(value, time, variables=None):
    if isinstance(value, (int, float)):
        return value
    s = value.strip()
    # Molang's nested conditional operator has no Python equivalent.
    while s.startswith('(') and s.endswith(')'):
        depth = 0
        whole = True
        for i, char in enumerate(s):
            depth += (char == '(') - (char == ')')
            if depth == 0 and i < len(s)-1:
                whole = False
                break
        if not whole:
            break
        s = s[1:-1]
    depth = 0
    question = colon = None
    for i, char in enumerate(s):
        depth += (char == '(') - (char == ')')
        if depth == 0 and char == '?':
            question = i
        if depth == 0 and char == ':' and question is not None:
            colon = i
            break
    if question is not None:
        assert colon is not None
        branch = s[question+1:colon] if evaluate(s[:question], time, variables) else s[colon+1:]
        return evaluate(branch, time, variables)
    parsed = ast.parse(s, mode='eval')
    allowed = (ast.Expression, ast.BinOp, ast.UnaryOp, ast.Constant, ast.Name,
               ast.Load, ast.Call, ast.Attribute, ast.Add, ast.Sub, ast.Mult,
               ast.Div, ast.USub, ast.UAdd, ast.Compare, ast.Lt)
    assert all(isinstance(n, allowed) for n in ast.walk(parsed)), s
    for node in ast.walk(parsed):
        if isinstance(node, ast.Attribute):
            assert isinstance(node.value, ast.Name) and node.value.id in ('math', 'query', 'variable')
            assert not node.attr.startswith('_')
    functions = SimpleNamespace(sin=lambda x: math.sin(math.radians(x)),
                                cos=lambda x: math.cos(math.radians(x)),
                                abs=abs, pow=pow, min=min, max=max,
                                clamp=lambda v, lo, hi: min(hi, max(lo, v)),
                                lerp=lambda a, b, t: a+(b-a)*t)
    return eval(compile(parsed, 'hummingbird-expression', 'eval'),
                {'__builtins__': {}}, {'math': functions,
                'query': SimpleNamespace(anim_time=time),
                'variable': SimpleNamespace(**dict(dict(backward_blend=0,wing_stroke_time=(variables or {}).get('flight_time',0),wing_yaw_bias=0),**(variables or {})))})


def author_vector(frames, time, expression_time=None):
    if not frames:
        raise AssertionError('Missing source frames')
    expression_time = time if expression_time is None else expression_time
    def point(i):
        return [evaluate(frames[i]['data_points'][0][a], expression_time) for a in 'xyz']
    if time <= frames[0]['time']:
        return point(0)
    if time >= frames[-1]['time']:
        return point(len(frames)-1)
    i = next(i for i in range(len(frames)-1) if time <= frames[i+1]['time'])
    t = (time-frames[i]['time'])/(frames[i+1]['time']-frames[i]['time'])
    p1, p2 = point(i), point(i+1)
    if frames[i]['interpolation'] == 'linear' and frames[i+1]['interpolation'] == 'linear':
        return [a+(b-a)*t for a, b in zip(p1, p2)]
    assert frames[i]['interpolation'] == frames[i+1]['interpolation'] == 'catmullrom'
    p0, p3 = point(max(0, i-1)), point(min(len(frames)-1, i+2))
    h00, h10 = 2*t**3-3*t**2+1, t**3-2*t**2+t
    h01, h11 = -2*t**3+3*t**2, t**3-t**2
    return [h00*b+h10*(c-a)/2+h01*c+h11*(d-b)/2 for a, b, c, d in zip(p0, p1, p2, p3)]


def bedrock(vector, channel):
    # These values are already Bedrock exports; only GeckoLib's actual baker
    # applies the later rotation sign conversion in the Java regression.
    return list(vector)


def exported_vector(track, time, variables=None):
    if isinstance(track, list):
        return [evaluate(v, time, variables) for v in track]
    if 'vector' in track:
        return exported_vector(track['vector'], time, variables)
    if 'post' in track:
        return exported_vector(track['post'], time, variables)
    frames = sorted((float(t), v) for t, v in track.items())
    before = max((f for f in frames if f[0] <= time), default=frames[0], key=lambda f:f[0])
    after = min((f for f in frames if f[0] >= time), default=frames[-1], key=lambda f:f[0])
    a, b = exported_vector(before[1], time, variables), exported_vector(after[1], time, variables)
    t = (time-before[0])/(after[0]-before[0]) if after[0] > before[0] else 0
    return [x+(y-x)*t for x, y in zip(a, b)]


def frames_by_bone(animation):
    def vector(frame):
        if isinstance(frame,list):
            return frame
        if 'vector' in frame:
            return vector(frame['vector'])
        return vector(frame['post'])
    result = {}
    for bone, channels in animation['bones'].items():
        result[bone] = {}
        for channel, track in channels.items():
            entries = [('0',track)] if isinstance(track,list) or 'vector' in track else sorted(track.items(),key=lambda item:float(item[0]))
            result[bone][channel] = [dict(time=float(time),
                 interpolation=frame.get('lerp_mode','linear') if isinstance(frame,dict) else 'linear',
                 data_points=[dict(zip('xyz',vector(frame)))]) for time,frame in entries]
    return result


def samples_for(frames):
    if len(frames) == 1:
        return [0, .03125, .0625, .125, .5, 1.75, 3.5]
    return sorted({frames[i]['time']+(frames[i+1]['time']-frames[i]['time'])*k/32
                   for i in range(len(frames)-1) for k in range(33)})


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('source',nargs='?',default=str(SOURCE_DIR))
    source_dir=Path(parser.parse_args().source)
    source_path=source_dir/SOURCE_FILES['animation']
    raw = source_path.read_bytes()
    source = json.loads(raw.decode('utf-8-sig'))
    source_assets={kind:dict(filename=name,sha256=hashlib.sha256((source_dir/name).read_bytes()).hexdigest()) for kind,name in SOURCE_FILES.items()}
    for kind,target in {'animation':'animations/hummingbird.animation.json','geometry':'geo/hummingbird.geo.json','texture':'textures/entity/hummingbird.png'}.items():
        assert (ASSETS/target).read_bytes()==(source_dir/SOURCE_FILES[kind]).read_bytes(),kind
    exported = json.loads((ASSETS/'animations/hummingbird_runtime.animation.json').read_text(encoding='utf8'))['animations']
    def reject_scientific_molang(value):
        if isinstance(value, dict):
            for child in value.values():
                reject_scientific_molang(child)
        elif isinstance(value, list):
            for child in value:
                reject_scientific_molang(child)
        elif isinstance(value, str) and any(prefix in value for prefix in ('math.', 'query.', 'variable.')):
            assert not re.search(r'(?<![A-Za-z0-9_.])(?:\d+(?:\.\d*)?|\.\d+)[eE][+-]?\d+',value), value
    reject_scientific_molang(exported)
    def check_stroke_clock(value):
        if isinstance(value,dict):
            for child in value.values():check_stroke_clock(child)
        elif isinstance(value,list):
            for child in value:check_stroke_clock(child)
        elif isinstance(value,str) and '2880' in value:
            assert 'variable.flight_time' not in value,value
    for name,clip in exported.items():
        if name.startswith(('animation.body.','animation.wings.','animation.eyes.')):
            check_stroke_clock(clip)
    indexed = {name:frames_by_bone(a) for name,a in source['animations'].items()}
    references = []
    count = 0
    max_error = 0
    for clip, animation in source['animations'].items():
        assert exported[clip].get('loop',False) == animation.get('loop',False), clip
        assert exported[clip].get('animation_length',0) == animation.get('animation_length',0), clip
        for bone, channels in indexed[clip].items():
            for channel, frames in channels.items():
                if not frames:
                    continue
                reference = []
                for time in samples_for(frames):
                    raw_expected = author_vector(frames,time)
                    expected = bedrock(raw_expected,channel)
                    actual = exported_vector(exported[clip]['bones'][bone][channel],time)
                    error = max(abs(a-b) for a,b in zip(actual,expected))
                    assert error < 1e-7, (clip,bone,channel,time,actual,expected,error)
                    max_error = max(max_error,error)
                    count += 1
                    reference.append(dict(time=time,bedrock_vector=expected))
                if frames[0]['interpolation']=='catmullrom':
                    references.append(dict(clip=clip,bone=bone,channel=channel,frames=[dict(time=f['time'],value=list(f['data_points'][0].values()),interpolation=f['interpolation']) for f in frames],samples=reference))
    hover, fly = indexed['animation.fly_idle'], indexed['animation.fly']
    for blend in (0,.25,.5,.75,1):
        for bone in BODY:
            for channel, default in DEFAULTS.items():
                h = hover.get(bone,{}).get(channel,[])
                f = fly.get(bone,{}).get(channel,[])
                times = samples_for(h) if h else [0,.03125,.5,1.75,3.5]
                for time in times:
                    phase = 64.03125+time
                    a = bedrock(author_vector(h,time,phase),channel) if h else default
                    b = bedrock(author_vector(f,time,phase),channel) if f else default
                    expected = [x+(y-x)*blend for x,y in zip(a,b)]
                    actual = exported_vector(exported['animation.body.flight']['bones'][bone][channel],time,
                                             dict(flight_time=11.125+time,wing_stroke_time=phase,forward_blend=blend,bank_angle=0))
                    error = max(abs(x-y) for x,y in zip(actual,expected))
                    assert error < 1e-7,(blend,bone,channel,time,actual,expected)
                    max_error = max(max_error,error)
                    count += 1
    # Check the actual hierarchy, not just signs of the two edited channels.
    # Forward flight must move the torso's head end towards the beak and make
    # the longitudinal axis more horizontal while keeping the beak level.
    from render_handbook_thumbnails import pos,rot,translate
    import numpy as np
    geometry = json.loads((ASSETS/'geo/hummingbird.geo.json').read_text(encoding='utf8'))['minecraft:geometry'][0]
    rig = {b['name']:b for b in geometry['bones']}
    def pose_geometry(blend,backward=0):
        transforms={}
        def visit(name):
            if name in transforms:
                return transforms[name]
            bone=rig[name];parent=bone.get('parent')
            tracks=exported['animation.body.flight']['bones'].get(name,{})
            values={c:exported_vector(track,.03125,dict(flight_time=.03125,forward_blend=blend,backward_blend=backward,bank_angle=0)) for c,track in tracks.items()}
            pp=pos(rig[parent]['pivot']) if parent else np.zeros(3)
            local=translate(pos(bone['pivot'])-pp+pos(values.get('position',[0,0,0])))
            local=local@rot(np.array(bone.get('rotation',[0,0,0]))+values.get('rotation',[0,0,0]))@np.diag([*values.get('scale',[1,1,1]),1])
            transforms[name]=(visit(parent) if parent else np.eye(4))@local
            return transforms[name]
        head,hip=visit('head'),visit('hip')
        axis=head[:3,3]-hip[:3,3]
        beak=head[:3,:3]@np.array([0,0,-1])
        return axis,beak
    hover_axis,_=pose_geometry(0)
    forward_axis,forward_beak=pose_geometry(1)
    hover_angle=math.degrees(math.atan2(abs(hover_axis[1]),abs(hover_axis[2])))
    forward_angle=math.degrees(math.atan2(abs(forward_axis[1]),abs(forward_axis[2])))
    assert forward_axis[2]<0 and forward_angle<hover_angle-20,(hover_axis,forward_axis)
    assert forward_beak[2]<-.99 and abs(forward_beak[1])<1e-7,forward_beak
    backward_axis,backward_beak=pose_geometry(0,1)
    _,hover_beak=pose_geometry(0)
    backward_angle=math.degrees(math.atan2(abs(backward_axis[1]),abs(backward_axis[2])))
    assert backward_angle>hover_angle and backward_angle>forward_angle+40,(hover_angle,backward_angle,forward_angle)
    # Authored yaw/roll micro-motions remain intact. Their Euler composition
    # makes the compensated direction differ slightly when a head turn is in
    # progress; reject visible attitude drift, not that original head motion.
    beak_drift=math.degrees(math.acos(float(np.clip(np.dot(backward_beak,hover_beak),-1,1))))
    assert beak_drift<.5,(backward_beak,hover_beak,beak_drift)
    for backward in (0,.25,.5,.75,1,2):
        for bone in ('all','head'):
            for time in (0,.03125,.5,1.75,3.5):
                frames=hover.get(bone,{}).get('rotation')
                expected=bedrock(author_vector(frames,time,64.03125+time),'rotation')
                expected[0]+=(-10 if bone=='all' else 10)*min(1,backward)
                actual=exported_vector(exported['animation.body.flight']['bones'][bone]['rotation'],time,
                    dict(flight_time=64.03125+time,forward_blend=0,backward_blend=backward,bank_angle=0))
                assert max(abs(a-b) for a,b in zip(actual,expected))<1e-7,(bone,backward,time,actual,expected)
                count+=1
    for bias in (-10,-4,-2,0,2,4,10):
        for time in (0,.03125,.5,1.75,3.5):
            for bone in WINGS:
                tracks=hover[bone]
                for channel in ('rotation','position'):
                    expected=bedrock(author_vector(tracks[channel],time,64.03125+time),channel)
                    if channel=='rotation':
                        expected[1]+=min(4,max(-4,bias))
                    actual=exported_vector(exported['animation.wings.fly']['bones'][bone][channel],time,
                        dict(flight_time=64.03125+time,wing_stroke_time=64.03125+time,wing_pitch=40,wing_yaw_bias=bias))
                    assert max(abs(a-b) for a,b in zip(actual,expected))<1e-7,(bone,bias,time,channel,actual,expected)
                    count+=1
    for suffix in ('idle_diff_1','idle_diff_2'):
        for bone, channels in indexed['animation.'+suffix].items():
            layer = 'body' if bone in BODY else 'wings' if bone in WINGS else 'eyes'
            runtime = exported['animation.'+layer+'.'+suffix]
            assert runtime['loop'] is False and runtime['animation_length']==4
            for channel, frames in channels.items():
                if not frames:
                    continue
                for time in samples_for(frames):
                    # Aerial body/wing waveforms share stroke phase, while the
                    # source clip timeline and low-frequency eye blink do not.
                    phase = 64.03125+time
                    expr_time = phase if suffix=='idle_diff_1' and layer!='eyes' else time
                    expected = bedrock(author_vector(frames,time,expr_time),channel)
                    actual = exported_vector(runtime['bones'][bone][channel],time,
                                             dict(flight_time=11.125+time,wing_stroke_time=phase,bank_angle=0))
                    error = max(abs(x-y) for x,y in zip(actual,expected))
                    assert error < 1e-7,(suffix,bone,channel,time,actual,expected)
                    max_error = max(max_error,error)
                    count += 1
    OUT.mkdir(parents=True,exist_ok=True)
    reference_path = OUT/'curve-reference.json'
    reference_path.write_text(json.dumps(dict(source=str(source_path),source_sha256=hashlib.sha256(raw).hexdigest(),
        curves=references),ensure_ascii=False,indent=2)+'\n',encoding='utf8')
    (OUT/'author-reference.json').write_text(json.dumps(dict(source_assets=source_assets,animations=source['animations']),
        ensure_ascii=False,indent=2)+'\n',encoding='utf8')
    report = dict(source_unchanged=source_path.read_bytes()==raw,source_assets=source_assets,
                  original_assets_byte_identical=True,clips_compared=6,analytic_spline_tracks=len(references),sampled_vectors=count,
                  max_absolute_error=max_error,forward_blends=[0,.25,.5,.75,1],
                  geometry_conversion=False,scientific_molang_literals=False,reference=str(reference_path))
    report['original_forward_pose']={'source_fly_all_x':47.5,'source_fly_head_x':-47.5,
                                    'secondary_coordinate_conversion':False,
                                    'hover_torso_angle_from_horizontal':hover_angle,
                                    'forward_torso_angle_from_horizontal':forward_angle,
                                    'beak_level':True}
    report['runtime_directional_pose']={'backward_torso_angle_from_horizontal':backward_angle,
                                        'backward_head_compensation':True,
                                        'backward_beak_direction_drift_degrees':beak_drift,
                                        'backward_pose_pitch_bound_degrees':10,
                                        'lateral_wing_yaw_bias_bound_degrees':4,
                                        'wing_clock':'Body/wing 8 Hz and 16 Hz waveforms share wing_stroke_time independently of age/clip clocks',
                                        'parameter_status':'Minecraft visual adaptation; not a physical aerodynamic model'}
    (OUT/'export-validation.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf8')
    print(json.dumps(report,ensure_ascii=False))


if __name__=='__main__':
    main()
