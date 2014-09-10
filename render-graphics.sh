#!/bin/bash

GIMP=$(which gimp)

set -e

if [ ! -x "$GIMP" ] ; then
    GIMP=$(mdfind 'kMDItemFSName = GIMP.app && kMDItemKind = Program')/Contents/MacOS/gimp
fi

if [ ! -x "$GIMP" ] ; then
    echo ERROR: GIMP not found, please install GIMP
    exit 1
fi


# From: http://stackoverflow.com/a/5846727/473672
cd "$(dirname $0)"/gfx
"$GIMP" -n -i -b - <<EOF
(let* ( (file's (cadr (file-glob "*.xcf" 1))) (filename "") (image 0) (layer 0) )
  (while (pair? file's)
    (set! image (car (gimp-file-load RUN-NONINTERACTIVE (car file's) (car file's))))
    (set! layer (car (gimp-image-merge-visible-layers image CLIP-TO-IMAGE)))
    (set! filename (string-append (substring (car file's) 0 (- (string-length (car file's)) 4)) ".png"))
    (gimp-file-save RUN-NONINTERACTIVE image layer filename filename)
    (gimp-image-delete image)
    (set! file's (cdr file's))
    )
  (gimp-quit 0)
  )
EOF

RES=../src/main/res
mkdir -p $RES/drawable-xxhdpi
mkdir -p $RES/drawable-xhdpi
mkdir -p $RES/drawable-hdpi
mkdir -p $RES/drawable-mhdpi

# From http://lifehacker.com/5962420/batch-resize-images-quickly-in-the-os-x-terminal
PNG=ic_launcher.png
sips -Z 144 logo.png --out $RES/drawable-xxhdpi/$PNG
sips -Z  96 logo.png --out $RES/drawable-xhdpi/$PNG
sips -Z  72 logo.png --out $RES/drawable-hdpi/$PNG
sips -Z  48 logo.png --out $RES/drawable-mdpi/$PNG
