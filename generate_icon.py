from PIL import Image, ImageDraw, ImageFont
import os

# Doodle icon: squircle purple gradient + white doodle-style alarm (hand-drawn feel)
sizes = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}
base = r"D:\Doodle\app\src\main\res"

# Colors - doodle palette
BG1 = (124, 58, 237)  # violet
BG2 = (99, 102, 241)  # indigo
ACC = (251, 191, 36)  # amber for bell
WHITE = (255,255,255)

for folder, size in sizes.items():
    img = Image.new("RGBA", (size, size), (0,0,0,0))
    draw = ImageDraw.Draw(img)
    # Squircle bg with gradient
    for y in range(size):
        t = y/size
        r = int(BG1[0]*(1-t) + BG2[0]*t)
        g = int(BG1[1]*(1-t) + BG2[1]*t)
        b = int(BG1[2]*(1-t) + BG2[2]*t)
        draw.line([(0,y),(size,y)], fill=(r,g,b,255))
    # Round corners (squircle)
    mask = Image.new("L", (size,size), 0)
    md = ImageDraw.Draw(mask)
    rad = int(size*0.22)
    md.rounded_rectangle([0,0,size,size], radius=rad, fill=255)
    img.putalpha(mask)

    d = ImageDraw.Draw(img)
    cx, cy = size//2, size//2 + int(size*0.04)
    r = int(size*0.28)
    # Alarm body white stroke doodle
    # Bell
    d.ellipse([cx-r, cy-r, cx+r, cy+r], fill=WHITE, outline=(0,0,0,30), width=max(1,int(size*0.015)))
    # Inner shadow
    d.ellipse([cx-r+2, cy-r+2, cx+r-2, cy+r-2], outline=(124,58,237,60), width=max(1,int(size*0.02)))
    # Clock center
    cr = int(size*0.08)
    d.ellipse([cx-cr, cy-cr, cx+cr, cy+cr], fill=(30,30,35))
    # Hands
    d.line([cx, cy, cx+int(r*0.45), cy-int(r*0.1)], fill=(30,30,35), width=max(2,int(size*0.04)))
    d.line([cx, cy, cx, cy-int(r*0.5)], fill=(30,30,35), width=max(2,int(size*0.04)))
    # Bells on top
    br = int(size*0.12)
    d.ellipse([cx-r+int(r*0.15), cy-r-int(br*0.4), cx-r+int(r*0.15)+br*2, cy-r-int(br*0.4)+br], fill=ACC, outline=WHITE, width=max(1,int(size*0.02)))
    d.ellipse([cx+r-int(r*0.15)-br*2, cy-r-int(br*0.4), cx+r-int(r*0.15), cy-r-int(br*0.4)+br], fill=ACC, outline=WHITE, width=max(1,int(size*0.02)))
    # Doodle sparkles
    for sx, sy in [(cx-r-int(size*0.08), cy-int(size*0.15)), (cx+r+int(size*0.08), cy-int(size*0.1))]:
        d.line([sx-4, sy, sx+4, sy], fill=WHITE, width=max(1,int(size*0.02)))
        d.line([sx, sy-4, sx, sy+4], fill=WHITE, width=max(1,int(size*0.02)))

    # Save png
    out = os.path.join(base, folder, "ic_launcher.png")
    os.makedirs(os.path.dirname(out), exist_ok=True)
    img.save(out, "PNG")
    # Round as copy
    img.save(os.path.join(base, folder, "ic_launcher_round.png"), "PNG")
    print(f"Generated {folder} {size}px")

# Adaptive foreground (108dp) - vector fallback
# Create mipmap-anydpi if needed? Just use pngs via manifest
print("Done icon")
