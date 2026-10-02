"""Import the author's built website as offline guide data. Never contacts the website.

Usage: python tools/import_website_handbook.py F:/myweb
Requires the website's Node dependencies, BeautifulSoup and Pillow on the build machine only.
"""
import json
from pathlib import Path
import subprocess
import sys
from urllib.parse import urljoin, urlparse
from bs4 import BeautifulSoup, NavigableString
from PIL import Image
from handbook_overrides import apply_gameplay_overrides

web = Path(sys.argv[1]).resolve()
repo = Path(__file__).resolve().parents[1]
assets = repo / 'src/main/resources/assets/guaniao'
module = (web / 'content/items.mjs').as_uri()
js = f"import {{items,catalogItems,itemCategories}} from {json.dumps(module)}; console.log(JSON.stringify({{items,catalog:catalogItems.map(i=>i.id),categories:itemCategories}}));"
meta = json.loads(subprocess.check_output(['node', '--input-type=module', '-e', js], encoding='utf-8'))
birds = json.loads((web/'content/bird-reference.json').read_text(encoding='utf-8'))['birds']
base = '/mods/bird-watching/'
docs = [('photography', 'nikon_d750'), ('recipes', 'crafting_table'),
        ('sky', 'feather'), ('settings', 'comparator'), ('server', 'chest'), ('troubleshooting', 'book')]
definitions = []
for b in birds:
    definitions.append(dict(id='bird/'+b['id'], route=base+'birds/'+b['slug']+'/minecraft/',
                            kind='bird', category='birds', model=b['id'], icon='', catalog=True))
for i in meta['items']:
    definitions.append(dict(id='item/'+i['id'], route=base+'items/'+i['slug']+'/', kind='item',
                            category=i['category'], model='', icon=i['icon'], catalog=i['id'] in meta['catalog']))
for slug, icon in docs:
    definitions.append(dict(id='doc/'+slug, route=base+'docs/'+slug+'/', kind='doc', category='docs',
                            model='', icon=('guaniao:' if icon=='nikon_d750' else 'minecraft:')+icon, catalog=True))
routes = {d['route']:d['id'] for d in definitions}
routes[base+'docs/birds/'] = 'birds'
routes[base+'items/'] = 'items'
routes[base+'docs/'] = 'docs'

def link(href):
    if href.startswith('/en/'):
        href = href[3:]
    if href.startswith('#'):
        return href
    if not href.startswith(('/', 'https://', 'http://')):
        href = urljoin(current_route, href)
    if href.startswith('https://www.findedd.cn/'):
        parsed = urlparse(href)
        href = parsed.path + ('#'+parsed.fragment if parsed.fragment else '')
    path, _, anchor = href.partition('#')
    if path == base+'docs/getting-started/':
        return 'docs'
    if path == base+'docs/birds/' and anchor:
        return 'bird/'+anchor.replace('-', '_')
    if path.startswith(base+'items/bird-dropping-'):
        return 'item/bird_droppings#types'
    if path in routes:
        return routes[path] + ('#'+anchor if anchor else '')
    return ('https://www.findedd.cn'+href) if href.startswith('/') else href

def spans(node):
    result = []
    def walk(n, target=''):
        if isinstance(n, NavigableString):
            text = str(n).replace('\n', ' ')
            if text:
                result.append({'text':text, 'link':target})
            return
        if n.name in ['script','style','audio','img']:
            return
        target = link(n.get('href','')) if n.name=='a' else target
        for child in n.children:
            walk(child, target)
    walk(node)
    return result

def extract(node):
    blocks = []
    def emit(kind, n, **extra):
        blocks.append(dict(type=kind, id=n.get('id',''), spans=spans(n), **extra))
    def walk(n):
        if isinstance(n, NavigableString):
            if str(n).strip():
                blocks.append(dict(type='p', id='', spans=[{'text':str(n).strip(),'link':''}]))
            return
        classes = n.get('class', [])
        if n.name in ['script','style','img','audio'] or 'article-sources' in classes:
            return
        if 'recipe-entry' in classes:
            blocks.append(dict(type='recipe', id=n.get('id',''), spans=[], recipe='guaniao:'+n['id']))
            return
        if 'fan-anvil' in classes:
            slots=n.select('[data-item]')
            if len(slots)!=3 or not slots[1]['data-item'].endswith('_book'):
                raise ValueError('Unrecognized feather-fan anvil diagram')
            blocks.append(dict(type='anvil', id=n.get('id',''), spans=[], book=slots[1]['data-item']))
            return
        if n.name in ['h2','h3','h4','p','li','figcaption','summary']:
            emit('h3' if n.name in ['h3','h4','summary'] else 'p' if n.name=='figcaption' else n.name, n)
            return
        if n.name=='tr':
            cells = n.find_all(['th','td'], recursive=False)
            blocks.append(dict(type='p',id=n.get('id',''),spans=[{'text':'  /  '.join(c.get_text(' ',strip=True) for c in cells),'link':''}]))
            return
        if n.name=='a':
            emit('link',n)
            return
        if n.name=='div' and n.get('id'):
            blocks.append(dict(type='anchor', id=n['id'], spans=[]))
        if n.name=='figure' and n.find('audio'):
            emit('h3', n.find('figcaption') or n)
            src=n.find('audio').get('src','')
            if '/calls/cockatiel/' in src:
                blocks.append(dict(type='sound',id='',spans=[],sound='guaniao:guide.cockatiel.'+Path(src).stem))
            return
        for child in n.children:
            walk(child)
    walk(node)
    return blocks

for lang, prefix in [('zh_cn',''), ('en_us','en/')]:
    pages=[]
    for d in definitions:
        current_route=d['route']
        source=web/'dist'/prefix/d['route'].lstrip('/')/'index.html'
        if not source.exists():
            raise RuntimeError('Missing localized page: '+str(source))
        soup=BeautifulSoup(source.read_text(encoding='utf-8'),'html.parser')
        article=soup.select_one('article.doc-content')
        title=article.find('h1').get_text(' ',strip=True)
        heading=article.select_one('.bird-game-heading, .item-heading')
        summary=heading.find('p',class_=lambda c:c!='item-version').get_text(' ',strip=True) if heading else ''
        facts=[{'label':e.dt.get_text(' ',strip=True),'text':e.dd.get_text(' ',strip=True)} for e in article.select('.bird-facts > div')]
        sections=[]
        for section in article.find_all('section',recursive=False):
            h=section.find(['h2','h3'])
            if not h: continue
            section_title=h.get_text(' ',strip=True)
            h.extract()
            sections.append(dict(id=section.get('id',''),title=section_title,blocks=extract(section)))
        if not sections:
            raise RuntimeError('No sections: '+str(source))
        pages.append(dict(**d,title=title,summary=summary,facts=facts,sections=sections))
    apply_gameplay_overrides(pages, lang)
    en=json.loads((web/'content/i18n/core.en.json').read_text(encoding='utf-8')) if lang=='en_us' else {}
    en.update(json.loads((web/'content/i18n/items.en.json').read_text(encoding='utf-8')) if lang=='en_us' else {})
    categories=[{'id':c['id'],'title':en.get(c['name'],c['name'])} for c in meta['categories']]
    out=assets/'guide'/f'{lang}.json'
    out.parent.mkdir(parents=True,exist_ok=True)
    out.write_text(json.dumps(dict(schema=1,pages=pages,categories=categories),ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(lang, len(pages), 'pages,', sum(len(p['sections']) for p in pages), 'sections')

if '--data-only' not in sys.argv:
    textures=assets/'textures/gui/handbook'
    textures.mkdir(parents=True,exist_ok=True)
    # Website posters can contain authoring-tool selection edges. Render clean original rigs instead.
    from render_handbook_thumbnails import render_all
    render_all()
    with Image.open(web/'dist/assets/bird-watching/field/woodland-1920.webp') as im:
        im.convert('RGB').resize((1280,720),Image.Resampling.LANCZOS).save(textures/'forest.png')
