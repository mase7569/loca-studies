package map;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
import java.awt.Color;
import java.util.Arrays;
import java.awt.Graphics2D;
import java.util.Arrays;
import java.io.IOException;
import java.util.LinkedList;

/**
 * An image made up of tiles. Tiles are saved on hdd and loaded
 * into memory when necessary. Only one tile is cached in memory
 * at a time. The image is not editable.
 *
 * The dims of the tiles are unspecified, but always follows:
 *  -All are perfect squares with same width/height, except
 *  -Last column tiles may be thinner,
 *  -Last row tiles may be shorter.
 *
 * Tiles are saved in png-format in given directory, following
 * the naming convention: tile-r-c.png.
 */
public class TiledImage {

    /**
     * Directory where tiles reside. */
    public/***/ final Path dir;

    /**
     * Tile in memory (cached), at specified row and column.
     * Only one cached tile at the time: last loaded tile. */
    public/***/ BasicImage memTile;
    public/***/ int memTileRow;
    public/***/ int memTileCol;

    /**
     * Image data, so don't have to load and investigate.
     * Note: last-column-width = width % tileLength
     *       last-row-height = height % tileLength.
     * also:
     * tileWidth == tileHeight except if one-row/col layout. */
    public/***/ final int width, height, tileWidth, tileHeight, rows, cols;

    /**
     * Constructs the tiledImage from saved tiles.
     *
     * @param dir Directory where tiles reside, correctly named.
     * @param w Width of image.
     * @param h Height of image.
     * @param tw Tile width of all tiles except those in last column.
     * @param th Tile height of all tiles except those in last row.
     * @param rs Number of rows in tile-layout.
     * @param cs Number of columns in tile-layout.
     */
    public/***/ TiledImage(Path dir, int w, int h, int tw, int th, int rs, int cs) throws IOException {
        this.dir = dir;
        this.width = w;
        this.height = h;
        this.tileWidth = tw;
        this.tileHeight = th;
        this.rows = rs;
        this.cols = cs;

        this.memTile = loadTile(0, 0, dir);
        this.memTileRow = 0;
        this.memTileCol = 0;
    }


    /**
     * @return Dir where tiles are stored.
     */
    public Path getDir() {
        return this.dir;
    }

    /**
     * @return Width of image.
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * @return Height of image.
     */
    public int getHeight() {
        return this.height;
    }

    /**
     * @return Width of all tiles except last column.
     */
    public int getTileWidth() {
        return this.tileWidth;
    }

    /**
     * @return Height of all tiles except last row.
     */
    public int getTileHeight() {
        return this.tileHeight;
    }

    /**
     * @return Width of last column tiles.
     */
    public/***/ int getLastColWidth() {
        return getWidth() - getTileWidth() * (this.cols - 1);
    }

    /**
     * @return Height of last row tiles.
     */
    public/***/ int getLastRowHeight() {
        return getHeight() - getTileHeight() * (this.rows - 1);
    }

    /**
     * @return Color of specified point.
     */
    public Color getColor(int[] p) throws IOException {
        int[] rc_xy = getTileAndPos(p);
        BasicImage tile = getTile(rc_xy[0], rc_xy[1]);
        return tile.getColor(rc_xy[2], rc_xy[3]);
    }

    /**
     * @return True if point p is inside image.
     */
    public boolean isInside(int[] p) {
        return
            p[0] >= 0 &&
            p[1] >= 0 &&
            p[0] < getWidth() &&
            p[1] < getHeight();
    }

    /**
     * Crops out a subimage. Tiles are concatenated as needed.
     * Beware of heap overflows. ~Minimizes no. reads from hdd.
     *
     * @param bs [xmin, ymin, xmax, ymax]. If outside, snaped inside.
     * @return A subimage defined by bounds.
     * @pre Min-bounds < max-bounds.
     */
    public BasicImage getSubImage(int[] bs) throws IOException {
        bs = Math2.getInsideBounds(bs, getWidth(), getHeight());
        BasicImage tiles = getSubImage_fullTiles(bs);
        int[] tl_rcxy = getTileAndPos(new int[]{bs[0], bs[1]});
        int xmin = tl_rcxy[2];
        int ymin = tl_rcxy[3];
        int xmax = xmin + (bs[2] - bs[0]);
        int ymax = ymin + (bs[3] - bs[1]);
        return tiles.getSubImage(xmin, ymin, xmax, ymax);
    }

    /**
     * @return Sub-image split in full tile-blocks where bounds fit.
     */
    public/***/ BasicImage getSubImage_fullTiles(int[] bs) throws IOException {
        LinkedList<LinkedList<BasicImage>> tiles = new LinkedList<LinkedList<BasicImage>>();
        int[] tl_rcxy = getTileAndPos(new int[]{bs[0], bs[1]});

        int xmin = bs[0] - tl_rcxy[2];
        int ymin = bs[1] - tl_rcxy[3];

        for (int y = ymin; y <= bs[3]; y += getTileHeight()) {
            LinkedList<BasicImage> row = new LinkedList<BasicImage>();

            for (int x = xmin; x <= bs[2]; x += getTileWidth()) {
                int[] rc = getTileAndPos(new int[]{x, y});
                BasicImage tile = getTile(rc[0], rc[1]);
                row.add(tile);
            }

            tiles.add(row);
        }

        return BasicImage.concatenateImages(tiles);
    }

    // /**
    //  * Creates a new image made up of all letters in the layout
    //  * on a straight line, in correct label-order.
    //  *
    //  * @param lay Label-layout.
    //  * @param spaceLen Horizontal space between letters.
    //  * @return One-line letter-image with hight of tallest letter-img.
    //  */
    // public BasicImage extractLabel(LabelLayout lay, int spaceLen) throws IOException {
    //     LinkedList<BasicImage> ls = extractLetters(lay);
    //     BasicImage img = BasicImage.concatenateImages(ls, spaceLen);
    //     img = img.addAlpha(100);
    //     //img = img.addBackground(Color.WHITE);

    //     //img.save("bla.png");
    //     return img;
    // }

    // /**
    //  * Uses default padding = average box height / 3.
    //  *
    //  * @param lay Label-layout.
    //  * @return One-line letter-image with hight of tallest letter-img.
    //  */
    // public BasicImage extractLabel(LabelLayout lay) throws IOException {
    //     int s = Math2.toInt(lay.getAverageBoxHeight() / 3);
    //     return extractLabel(lay, s);
    // }

    // /**
    //  * @return All letter-images in lay without rotation.
    //  * @pre lay describes a label in this image.
    //  */
    // public LinkedList<BasicImage> extractLetters(LabelLayout lay) throws IOException {
    //     LinkedList<BasicImage> ls = new LinkedList<BasicImage>();

    //     for (Box b : lay.getBoxesWithNewlines()) {
    //         if (b != null) {
    //             ls.add(extractElement(b));
    //         }
    //         else {
    //             int h = Math2.toInt(lay.getAverageBoxHeight());
    //             int w = Math2.toInt(h * 0.7f);
    //             BasicImage space = new BasicImage(w, h);
    //             ls.add(space);
    //         }
    //     }
    //     return ls;
    // }

    // /**
    //  * Returns an element in the image contained inside a box.
    //  * The box (and element in image) may be rotated, but returned
    //  * element is not.
    //  *
    //  * @param b Box describing element to be extracted.
    //  * @return A new image where non-rotated element fits perfectly,
    //  * i.e an un-rotated subsection of this image.
    //  */
    // public BasicImage extractElement(Box box) throws IOException {
    //     int[] bs = Math2.toIntBounds(box.getBounds());
    //     BasicImage rotated = getSubImage(bs);
    //     BasicImage straight = rotated.rotate(-box.getRotation());
    //     double w = box.getWidth();
    //     double h = box.getHeight();
    //     double x0 = (straight.getWidth() - w) / 2;
    //     double y0 = (straight.getHeight() - h) / 2;
    //     double[] bs_ = new double[]{x0, y0, x0+w, y0+h};
    //     return straight.getSubImage(Math2.toIntBounds(bs_));
    // }

    /**
     * Returns specified tile. If tile not in memory, loads from
     * hdd. NOTE: Returned tile is cached.
     *
     * @param r Row.
     * @param c Column.
     * @return Tile at [r,c].
     */
    public/***/ BasicImage getTile(int r, int c) throws IOException {
        if (r == this.memTileRow && c == this.memTileCol)
            return this.memTile;

        BasicImage tile = loadTile(r, c);
        cache(r, c, tile);
        return tile;
    }

    /**
     * Caches specified tile.
     */
    public/***/ void cache(int r, int c, BasicImage tile) {
        this.memTileRow = r;
        this.memTileCol = c;
        this.memTile = tile;
    }

    /**
     * @return Tile at specified row/column loaded from hdd.
     */
    public/***/ static BasicImage loadTile(int r, int c, Path dir) throws IOException {
        return BasicImage.load(getTilePath(r, c, dir));
    }
    public/***/ BasicImage loadTile(int r, int c) throws IOException {
        return loadTile(r, c, this.dir);
    }

    /**
     * @return Path to tile: x/y/tile-r-c.png
     */
    public/***/ static Path getTilePath(int r, int c, Path dir) {
        String fn = String.format("tile-%s-%s.png", r, c);
        return dir.resolve(fn);
    }
    public/***/ Path getTilePath(int r, int c) {
        return getTilePath(r, c, this.dir);
    }

    /**
     * @return [r, c, x, y] for row/col of tile containing point p,
     * and x,y local point in this tile.
     */
    public/***/ int[] getTileAndPos(int[] p) {
        if (!isInside(p)) throw new RuntimeException("Out of bounds");
        int r = p[1] / this.tileHeight;
        int c = p[0] / this.tileWidth;
        int x = p[0] % this.tileWidth;
        int y = p[1] % this.tileHeight;
        return new int[]{r, c, x, y};
    }

    /**
     * Loads a tiled image from a directory. Investigates tile-layout
     * through file-names and loads top-left and bottom-right tiles
     * for finding dims etc.
     *
     * @parm dir Directory where tiles resides.
     * @param Loaded TileImage with top-left tile as memTile.
     * @throws RuntimeException if bad tiles in directory.
     */
    public static TiledImage load(Path dir) throws IOException {
        if (!dir.toFile().isDirectory())
            throw new IOException("Bad dir");

        File[] fs = dir.toFile().listFiles();
        if (fs.length == 0)
            throw new IOException("Empty dir");

        Arrays.sort(fs);

        String fnTL = fs[0].getName();
        String fnBR = fs[fs.length - 1].getName();
        int[] rcTL = getRowCol(fnTL);
        int[] rcBR = getRowCol(fnBR);

        BasicImage tl = loadTile(rcTL[0], rcTL[1], dir);
        BasicImage br = loadTile(rcBR[0], rcBR[1], dir);

        int tileW = tl.getWidth();
        int tileH = tl.getHeight();
        int rows = rcBR[0] + 1;
        int cols = rcBR[1] + 1;
        int width = tileW * (cols - 1) + br.getWidth();
        int height = tileH * (rows - 1) + br.getHeight();

        return new TiledImage(dir, width, height, tileW, tileH, rows, cols);
    }
    public static TiledImage load(String dir) throws IOException {
        return load(Paths.get(dir));
    }

    /**
     * @param fn FileName: tile-r-c.png
     * @return [r, c]
     */
    public/***/ static int[] getRowCol(String fn) throws IOException {
        try {
            String[] ps = fn.split("[-.]");
            int r = Integer.parseInt(ps[1]);
            int c = Integer.parseInt(ps[2]);
            return new int[]{r, c};
        }
        catch (Exception e) {
            throw new IOException("Bad file name");
        }
    }

    /**
     * Delets this tiled-image from file. Don't use the reference
     * anymore!
     */
    public void delete() {
        deleteDir(getDir().toFile());
    }

    /**
     * Deletes all content in dir.
     */
    public/***/ static void cleanDir(File dir) {
        if (!dir.isDirectory()) return;

        for (File f : dir.listFiles()) {
            if (f.isDirectory()) cleanDir(f);
            else f.delete();
        }
    }

    /**
     * Deletes dir with all content.
     */
    public/***/ static void deleteDir(File dir) {
        if (!dir.isDirectory()) return;
        cleanDir(dir);
        dir.delete();
    }



    //---------------------------------------------------for testing

    /**
     * Assemble and save img. Be careful with heap overflows.
     */
    public void save(Path p) {
        getOneImage().save_(p);
    }
    public void save(String fn) {
        save(Paths.get(fn));
    }

    /**
     * @return All tiles concatenated into one basicImage. Be careful
     * with heap overflows.
     */
    public BasicImage getOneImage() {
        BasicImage[][] lay = new BasicImage[this.rows][this.cols];

        for (int r = 0; r < this.rows; r++) {
            for (int c = 0; c < this.cols; c++) {
                try {
                    lay[r][c] = loadTile(r, c);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return BasicImage.concatenateImages(lay);
    }

    /**
     * Draw tile grid lines.
     */
    public BasicImage getOneImageWithGrid() {
        BasicImage img = getOneImage();
        Graphics2D g = img.createGraphics();
        g.setPaint(Color.RED);

        for (int r = 1; r <= this.rows; r++) {
            int y = getTileHeight() * r;
            if (r == rows) y = getTileHeight() * (r-1) + getLastRowHeight();
            g.drawLine(0, y, getWidth()-1, y);
        }

        for (int c = 1; c <= this.cols; c++) {
            int x = getTileWidth() * c;
            if (c == cols) x = getTileWidth() * (c-1) + getLastColWidth();
            g.drawLine(x, 0, x, getHeight()-1);
        }

        return img;
    }


    /**
     * Builds the TiledImage by saving tiles to files.
     */
    public static class Builder {
        public/***/ int c = 0;
        public/***/ int r = 0;

        public/***/ Path dir;
        public/***/ int rows;
        public/***/ int cols;

        public/***/ int tileW = -1;
        public/***/ int tileH = -1;
        public/***/ int lastColW = -1;
        public/***/ int lastRowH = -1;

        /**
         * Initiates builder AND DELETES ALL FILES IN dir.
         *
         * @param rs No rows in tile layout.
         * @param cs No columns in tile layout.
         * @param dir Direction where tiles will be saved.
         */
        public Builder(int rs, int cs, Path dir) {
            this.rows = rs;
            this.cols = cs;
            this.dir = dir;

            cleanDir(dir.toFile());
        }

        /**
         * Adds a tile to the builder by saving it to file with
         * correct name. Adds 'left-to-right, row-by-row'.
         *
         * @pre All tiles perfect squares with same dims, except
         * last column (may be thinner), last row (may be shorter).
         * @throws RuntimeException if bad dims.
         */
        public void add(BasicImage tile) throws IOException {
            testDims(tile);

            Path p = getTilePath(r, c, dir);
            tile.save(p);

            c++;
            if (c >= cols) {
                c = 0;
                r++;
            }
            if (r > rows) {
                throw new RuntimeException("Too many tiles");
            }
        }

        /**
         * @throws RuntimeException if inconsistent dims.
         */
        public/***/ void testDims(BasicImage tile) {
            // set dims
            if (r == 0 && c == 0) {
                tileW = tile.getWidth();
                tileH = tile.getHeight();
            }
            if (c == cols-1 && r == 0) lastColW = tile.getWidth();
            if (r == rows-1 && c == 0) lastRowH = tile.getHeight();

            // test dims
            if (c == cols-1) assertEq(tile.getWidth(), lastColW);
            else assertEq(tile.getWidth(), tileW);

            if (r == rows-1) assertEq(tile.getHeight(), lastRowH);
            else assertEq(tile.getHeight(), tileH);
        }
        public/***/ void assertEq(int x, int y) {
            if (x != y) throw new RuntimeException("Bad tile dims");
        }

        /**
         * @return The TiledImage-object.
         * @throws RuntimeException if excpected more/less tiles.
         */
        public TiledImage build() throws IOException {
            if (c != 0 || r != rows)
                throw new RuntimeException("Bad tile numbering");

            int w = tileW * (cols - 1) + lastColW;
            int h = tileH * (rows - 1) + lastRowH;

            return new TiledImage(dir, w, h, tileW, tileH, rows, cols);
        }
    }



    //-------------------------------------for testing

    /**
     * Load without exceptions.
     */
    public static TiledImage load_(String dir) {
        try {
            return load(dir);
        }
        catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
            return null;
        }
    }
}
