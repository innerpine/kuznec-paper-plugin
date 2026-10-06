"""Contact sheet: python3 sheet.py out.jpg cols size img1 img2 ... (labels = file names)"""
import sys, os
from PIL import Image, ImageDraw
out, cols, size, files = sys.argv[1], int(sys.argv[2]), int(sys.argv[3]), sys.argv[4:]
rows = (len(files) + cols - 1) // cols
sheet = Image.new('RGB', (cols * (size + 8) + 8, rows * (size + 30) + 8), '#222')
d = ImageDraw.Draw(sheet)
for k, f in enumerate(files):
    im = Image.open(f).convert('RGB').resize((size, size), Image.LANCZOS)
    x, y = 8 + (k % cols) * (size + 8), 8 + (k // cols) * (size + 30)
    sheet.paste(im, (x, y))
    d.text((x + 4, y + size + 6), os.path.basename(f), fill='#ddd')
sheet.save(out, quality=90)
