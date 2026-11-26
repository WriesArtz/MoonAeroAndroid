import math
def flower(cx,cy,r,col="#ff8fc0",center="#ffe27a"):
    out=[f'<g transform="translate({cx},{cy})">']
    for i in range(5):
        a=i*72
        out.append(f'<ellipse cx="0" cy="{-r*0.62:.1f}" rx="{r*0.42:.1f}" ry="{r*0.62:.1f}" fill="{col}" stroke="#ff5d9e" stroke-width="{r*0.06:.1f}" transform="rotate({a})"/>')
    out.append(f'<circle r="{r*0.28:.1f}" fill="{center}"/></g>')
    return ''.join(out)
def fish(x,y,s,body,tail,rot=0,flip=1):
    return (f'<g transform="translate({x},{y}) rotate({rot}) scale({s*flip},{s})">'
      f'<path d="M-16,0 C-10,-12 8,-13 16,0 C8,12 -10,12 -16,0 Z" fill="{body}"/>'
      f'<path d="M14,0 L28,-10 C25,-3 25,3 28,10 Z" fill="{tail}"/>'
      f'<path d="M-4,-10 C0,-17 8,-16 9,-9 Z" fill="{tail}"/>'
      f'<path d="M-9,-6 C-3,-11 5,-11 9,-7 C2,-8 -3,-8 -9,-6Z" fill="#fff" opacity=".5"/>'
      f'<circle cx="-8" cy="-2" r="2.2" fill="#fff"/><circle cx="-8.4" cy="-2" r="1.1" fill="#12263a"/></g>')
def bubble(x,y,r,o=1):
    return (f'<g opacity="{o}"><circle cx="{x}" cy="{y}" r="{r}" fill="url(#bub)" stroke="#fff" stroke-opacity=".85" stroke-width="{max(1,r*0.09):.1f}"/>'
            f'<ellipse cx="{x-r*0.35:.1f}" cy="{y-r*0.42:.1f}" rx="{r*0.28:.1f}" ry="{r*0.15:.1f}" fill="#fff" opacity=".9" transform="rotate(-35 {x-r*0.35:.1f} {y-r*0.42:.1f})"/></g>')
def weed(x,y,h,col="#3fbf6a"):
    return f'<path d="M{x},{y} C{x-14},{y-h*0.35} {x+14},{y-h*0.65} {x},{y-h} C{x+6},{y-h*0.6} {x-6},{y-h*0.4} {x+4},{y}Z" fill="{col}" opacity=".9"/>'
scene = lambda: ''.join([
  '<rect x="0" y="0" width="512" height="512" fill="url(#water)"/>',
  '<path d="M0,330 C80,310 160,345 260,325 C340,310 420,340 512,320 L512,512 L0,512Z" fill="url(#sand)"/>',
  weed(120,350,70),weed(138,352,45,"#2fa85a"),weed(300,352,60),weed(318,350,40,"#2fa85a"),weed(425,352,65),
  flower(190,338,22),flower(340,344,26,"#ff9ccb"),flower(262,350,16,"#ffb3d6"),flower(395,336,18),
  '<polygon points="90,0 150,0 110,512 30,512" fill="#fff" opacity=".10"/><polygon points="230,0 270,0 235,512 180,512" fill="#fff" opacity=".08"/>',
  fish(170,235,1.5,"#ffa53a","#ff7a1a",-6), fish(320,270,1.25,"#ffd84a","#f2a81a",4,-1), fish(255,175,0.9,"#ff8a6a","#e9553a",-12),
  bubble(210,200,9),bubble(225,170,6),bubble(300,215,7),bubble(380,190,8),bubble(150,290,6),
])
svg=f'''<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="512" height="512" viewBox="0 0 512 512">
<defs>
 <linearGradient id="water" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#9af0fa"/><stop offset=".45" stop-color="#2bb4e0"/><stop offset="1" stop-color="#0a5aa8"/></linearGradient>
 <linearGradient id="sand" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#f6e7b4"/><stop offset="1" stop-color="#d9b86a"/></linearGradient>
 <linearGradient id="shell" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#b8f6fb"/><stop offset=".45" stop-color="#47cfe6"/><stop offset="1" stop-color="#1590bd"/></linearGradient>
 <linearGradient id="face" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#ffffff"/><stop offset=".5" stop-color="#d7f0f6"/><stop offset="1" stop-color="#9fd3e2"/></linearGradient>
 <linearGradient id="chin" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#eaf9fc"/><stop offset="1" stop-color="#a8d9e6"/></linearGradient>
 <linearGradient id="handle" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#7fe6f2"/><stop offset="1" stop-color="#1aa5c8"/></linearGradient>
 <linearGradient id="spec" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="#fff" stop-opacity=".95"/><stop offset="1" stop-color="#fff" stop-opacity="0"/></linearGradient>
 <radialGradient id="bub" cx=".5" cy=".5" r=".5"><stop offset=".55" stop-color="#bff6ff" stop-opacity=".05"/><stop offset="1" stop-color="#e8fdff" stop-opacity=".55"/></radialGradient>
 <radialGradient id="spk" cx=".4" cy=".35" r=".7"><stop offset="0" stop-color="#8ff0f2"/><stop offset="1" stop-color="#139bb4"/></radialGradient>
 <clipPath id="clipShell"><path id="shellp" d="M268,132 C205,98 112,126 80,214 C54,286 80,364 150,396 C190,414 232,408 268,396 Z"/></clipPath>
 <clipPath id="clipScreen"><path d="M286,154 Q286,146 296,148 L424,166 Q434,168 434,178 L434,332 Q434,342 424,343 L296,356 Q286,357 286,348 Z"/></clipPath>
 <filter id="soft"><feGaussianBlur stdDeviation="5"/></filter>
 <filter id="soft2"><feGaussianBlur stdDeviation="2"/></filter>
</defs>
<!-- ground shadow -->
<ellipse cx="270" cy="438" rx="190" ry="20" fill="#021b3a" opacity=".35" filter="url(#soft)"/>
<!-- handle -->
<path d="M196,128 C190,52 340,50 346,148" fill="none" stroke="#0b6f96" stroke-width="30" stroke-linecap="round"/>
<path d="M196,128 C190,52 340,50 346,148" fill="none" stroke="url(#handle)" stroke-width="24" stroke-linecap="round"/>
<path d="M205,112 C210,66 320,64 330,118" fill="none" stroke="#fff" stroke-opacity=".7" stroke-width="5" stroke-linecap="round"/>
<!-- translucent shell with aquarium inside -->
<path d="M268,132 C205,98 112,126 80,214 C54,286 80,364 150,396 C190,414 232,408 268,396 Z" fill="url(#shell)"/>
<g clip-path="url(#clipShell)"><g transform="translate(-30,40) scale(.95)">{scene()}</g>
  <path d="M268,132 C205,98 112,126 80,214 C54,286 80,364 150,396 C190,414 232,408 268,396 Z" fill="url(#shell)" opacity=".12"/>
</g>
<path d="M268,132 C205,98 112,126 80,214 C54,286 80,364 150,396 C190,414 232,408 268,396 Z" fill="none" stroke="#e6fdff" stroke-opacity=".9" stroke-width="3"/>
<path d="M120,170 C150,128 205,116 252,138" fill="none" stroke="#fff" stroke-opacity=".85" stroke-width="9" stroke-linecap="round"/>
<path d="M92,236 C84,276 92,330 120,364" fill="none" stroke="#fff" stroke-opacity=".5" stroke-width="6" stroke-linecap="round"/>
<!-- front face -->
<path d="M262,126 Q262,116 274,118 L440,140 Q454,142 454,156 L454,366 Q454,380 440,381 L274,398 Q262,399 262,388 Z" fill="url(#face)"/>
<path d="M262,126 Q262,116 274,118 L440,140 Q454,142 454,156 L454,366 Q454,380 440,381 L274,398 Q262,399 262,388 Z" fill="none" stroke="#fff" stroke-width="3" stroke-opacity=".9"/>
<!-- screen bezel + screen -->
<path d="M278,142 Q278,134 288,136 L430,156 Q442,158 442,170 L442,340 Q442,352 430,353 L288,366 Q278,367 278,358 Z" fill="#2a6f8c" opacity=".55"/>
<path d="M286,154 Q286,146 296,148 L424,166 Q434,168 434,178 L434,332 Q434,342 424,343 L296,356 Q286,357 286,348 Z" fill="#0a4f9a"/>
<g clip-path="url(#clipScreen)"><g transform="translate(232,138) scale(.5)">{scene()}</g></g>
<path d="M286,154 Q286,146 296,148 L424,166 Q434,168 434,178 L434,206 C380,224 330,236 286,262 Z" fill="#fff" opacity=".22"/>
<path d="M286,154 Q286,146 296,148 L424,166 Q434,168 434,178 L434,332 Q434,342 424,343 L296,356 Q286,357 286,348 Z" fill="none" stroke="#073a73" stroke-opacity=".8" stroke-width="3"/>
<!-- chin: speakers, slot -->
<circle cx="304" cy="378" r="10" fill="url(#spk)" stroke="#0e7d94" stroke-width="2"/>
<circle cx="428" cy="366" r="10" fill="url(#spk)" stroke="#0e7d94" stroke-width="2"/>
<path d="M326,377 L404,369 C410,369 410,375 404,375 L326,383 C320,383 320,377 326,377Z" fill="#0b6a86" opacity=".6"/>
<circle cx="366" cy="360" r="3.5" fill="#fff" opacity=".9"/>
<!-- feet -->
<path d="M150,396 L206,404 L204,418 L150,412 Z" fill="#0d7893"/><path d="M392,382 L440,380 L440,394 L392,398 Z" fill="#0d7893"/>
<!-- face specular -->
<path d="M270,124 L440,146 Q448,148 448,158 L448,176 Q360,160 272,150 Z" fill="#fff" opacity=".55"/>
<!-- bubbles around -->
{bubble(90,150,20)}{bubble(58,290,11)}{bubble(480,120,15)}{bubble(472,420,13)}{bubble(128,436,9)}{bubble(34,198,8)}{bubble(486,260,8)}
</svg>'''
open('rich.svg','w').write(svg)
