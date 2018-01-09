import json
import sys

def setName(data, name):
    data['name'] = name

def getLayers(data, types):
    filtered = []
    for layer in data['layers']:
        if layer['type'] in types:
            filtered.append(layer)
    return filtered

def getSymbolLayers(data):
    return getLayers(data, ['symbol'])

def getNonSymbolLayers(data):
    return getLayers(data, ['fill', 'line', 'circle', 'heatmap', 'fill-extrusion', 'raster', 'background'])

def setFont(data, font):
    data['glyphs'] = "mapbox://fonts/masel/{fontstack}/{range}.pbf"

    for layer in getSymbolLayers(data):
        layer['layout']['text-font'] = [font]


def setLetterSpacing(data, extraSpace=0):
    for layer in getSymbolLayers(data):
        space = extraSpace
        if 'text-letter-spacing' in layer['layout']:
            space += layer['layout']['text-letter-spacing']
        layer['layout']['text-letter-spacing'] = space

def undecorateText(data):
    for layer in getSymbolLayers(data):
        paint = layer['paint']
        paint['icon-opacity'] = 0
        paint['text-opacity'] = 1
        paint['text-color'] = 'hsla(0, 0%, 0%, 1)'
        paint['text-halo-color'] = 'hsla(0, 0%, 0%, 0)'

def hideGraphics(data):
    for layer in getNonSymbolLayers(data):
        layer['layout']['visibility'] = 'none'

def dumpStyle(data, fileName):
    f = open(fileName + ".json", 'w')
    f.write(json.dumps(data, indent=4))
    f.close()


font = 'Cousine Regular'
font_box = font + '-Box'
extraLetterSpace = 0.1

fileName_full = "full"
fileName_label = "label"
fileName_box = "box"

f = open(sys.argv[1], 'r')
data = json.load(f)

setFont(data, font)
setLetterSpacing(data, extraLetterSpace)
#noAbbreviations(data)
#noRoadSigns()
#noJunkLabels()
#setLanguage(data)
setName(data, fileName_full)
dumpStyle(data, fileName_full)

undecorateText(data)
hideGraphics(data)
setName(data, fileName_label)
dumpStyle(data, fileName_label)

setFont(data, font_box)
setName(data, fileName_box)
dumpStyle(data, fileName_box)
