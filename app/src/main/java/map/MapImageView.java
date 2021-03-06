package map;

import java.util.Arrays;

/**
 * A representation of a subsection of an image containing a map.
 * Enables easy convertion between image-pixel and geo-coordinates.
 * Immutable. Lon(gitude) and lat(itude) defines the position of
 * the center-point of the image. (Web-)Mercator-projection.
 * (0,0) is equator around Africa. Lon grows east, lat grows north.
 *
 * @inv -180 <= lon < 180
 * @inv -85 < lat < 85 (2*arctan(e^pi) - pi/2)
 * @inv width/height > 0
 * @inv 0 <= zoom <= 20
 * @inv lat-bounds inside +-85...
 */
public class MapImageView {

    /** Tile size in pixels (x*x) where zoom level 0 fits whole
     * world on one tile. */
    public static final int DEFAULT_TILE_SIZE = 512;

    /** Max/min value for latitude. */
    public static final double LATITUDE_BOUND =
        Math.toDegrees(2*Math.atan(Math.exp(Math.PI)) - Math.PI/2);

    /** Map-center longitude (degrees). */
    public final double lon;

    /** Map-center latitude (degrees). */
    public final double lat;

    /** Map width in pixels. Actual width depends on pixel-density. */
    public final int width;

    /** Map height in pixels. Actual height depends on pixel-density. */
    public final int height;

    /** Zoom of image where 0 is all way out and 20 is all way in. */
    public final int zoom;

    /** True means pixel-density is doubled on widht and hight. */
    public final boolean x2;


    /**
     * Default constructor.
     *
     * @param lon Center longitude.
     * @param lat Center latitude.
     * @param w Pixel-width. w > 0.
     * @param h Pixel-height. h > 0.
     * @param z Zoom. 0 <= z <= 20.
     * @param x2 Double pixel-density.
     */
    public MapImageView(double lon, double lat, int w, int h, int z, boolean x2) {
        this.lon = lon;
        this.lat = lat;
        this.width = w;
        this.height = h;
        this.zoom = z;
        this.x2 = x2;

        assertWidth(w, z, x2);
        assertLatitude(lat, h, z, x2);
    }

    /**
     * Constructs from map-bounds.
     *
     * @param west Western longitude (minX).
     * @param south Southern latitude (minY).
     * @param north Northen latitude (maxX).
     * @param east Eastern longitude (maxY).
     */
    public MapImageView(double west, double south, double east, double north, int z, boolean x2) {
        west = Math2.toUnitDegrees(west);
        east = Math2.toUnitDegrees(east);
        if (north - south <= 0)
            throw new IllegalArgumentException("Bad latitude bounds");
        if (north > LATITUDE_BOUND || south < -LATITUDE_BOUND) {
            throw new IllegalArgumentException("Bad latitude bounds");
        }

        double[] tl  = getPixelCoordinates_global(west, north, z, x2);
        double[] br  = getPixelCoordinates_global(east, south, z, x2);
        double width = br[0]-tl[0] > 0 ?
            (br[0]-tl[0]) : (getGlobalPixelMax(z, x2)[0]-tl[0]+br[0]);
        double height = br[1] - tl[1];
        double midX = tl[0] + width/2;
        double midY = (tl[1] + br[1]) / 2;
        double[] midGeo = getGeoCoordinates_global(midX, midY, z, x2);

        this.lon = midGeo[0];
        this.lat = midGeo[1];
        this.width = Math.round((float)width);
        this.height = Math.round((float)height);
        this.zoom = z;
        this.x2 = x2;;

        assertWidth(this.width, z, x2);
        assertLatitude(this.lat, this.height, z, x2);
    }

    /**
     * Constructor with coordinates-array.
     */
    public MapImageView(double[] wsen, int z, boolean x2) {
        this(wsen[0], wsen[1], wsen[2], wsen[3], z, x2);
    }

    /**
     * Throws exception if requested map is wider then earth.
     *
     * @param w Width.
     * @param z Zoom.
     * @param x2 Double pixel-density.
     */
    public/***/ static void assertWidth(int w, int z, boolean x2) {
        int yMax = getGlobalPixelMax(z, x2)[1];
        if (w > yMax)
            throw new IllegalArgumentException("Bad width.");
    }

    /**
     * Throws RuntimeException if latitude bound is ouside valid span.
     *
     * @param lat Latitude.
     * @param h Height.
     * @param z Zoom.
     * @param x2 Double pixel-density.
     */
    public/***/ static void assertLatitude(double lat, int h, int z, boolean x2) {
        if (!okLatitudeBound(lat, h, z, x2)) {
            throw new IllegalArgumentException("Bad latitude bounds");
        }
    }

    /**
     * @param lat Latitude.
     * @param h Height.
     * @param z Zoom.
     * @param x2 Double pixel-density.
     * @return False if latitude bound is ouside valid span.
     */
    public/***/ static boolean okLatitudeBound(double lat, int h, int z, boolean x2) {
        double[] globMid = getPixelCoordinates_global(0, lat, z, x2);
        int yMin = Math.round( (float)(globMid[1] - h/2d) );
        int yMax = Math.round( (float)(globMid[1] + h/2d) );

        return (yMin >= 0 && yMax <= getGlobalPixelMax(z, x2)[1]);
    }

    /**
     * Get max pixel coordinates for a certain zoom, img quality.
     *
     * @param z Zoom level.
     * @param x2 Double pixel-density.
     * @return Max pixel coordinates [x,y].
     */
    public/***/ static int[] getGlobalPixelMax(int z, boolean x2) {
        double[] xy = getPixelCoordinates_global(179.999999999, -LATITUDE_BOUND, z, x2);
        return new int[]{ Math.round((float)xy[0]), Math.round((float)xy[1]) };
    }
    public/***/ int[] getGlobalPixelMax() {
        return getGlobalPixelMax(this.zoom, this.x2);
    }

    /**
     * @return [wLon, sLat, eLon, nLat]
     */
    public double[] getGeoBounds() {
        double[] nw = getGeoCoordinates(0, 0);
        double[] se = getGeoCoordinates(this.width, this.height);
        return new double[]{ nw[0], se[1], se[0], nw[1] };
    }

    /**
     * @return Geo-coordinates lon-lat center.
     */
    public double[] getGeoMid() {
        return new double[]{this.lon, this.lat};
    }

    /**
     * @return Local mid-point [x,y].
     */
    public double[] getPixelMid() {
        return new double[]{this.width / 2, this.height / 2};
    }

    /**
     * @return Global-pixel-midpoint [x,y].
     */
    private double[] getGlobalPixelMid() {
        return getPixelCoordinates_global(this.lon, this.lat);
    }

    /**
     * Finds the bounds of another view when it is placed inside
     * this one.
     * @return [xmin, ymin, xmax, ymax]. Returned points are inside
     * this view, even if other view extends outside.
     */
    public double[] getPixelBoundsOfOtherView(MapImageView other) {
        double[] bs0 = this.getGlobalPixelBounds();
        double[] bs1 = other.getGlobalPixelBounds();

        return new double[] {
            Math.max(bs1[0] - bs0[0], 0),
            Math.max(bs1[1] - bs0[1], 0),
            Math.min(bs1[2] - bs0[0], bs0[2]),
            Math.min(bs1[3] - bs0[1], bs0[3])
        };
    }

    /**
     * @returns Global pixel bounds [xmin ymin xmax ymax].
     * Unwrapped, i.e xmax might go beyond global-max-value,
     * xmin might be less than zero.
     */
    public/***/ double[] getGlobalPixelBounds() {
        double[] mid = getGlobalPixelMid();
        return new double[]{mid[0] - width/2,
                            mid[1] - height/2,
                            mid[0] + width/2,
                            mid[1] + height/2};

        // double[] wsen = getGeoBounds();
        // double[] tl = getPixelCoordinates_global(wsen[0], wsen[3]);
        // double[] br = getPixelCoordinates_global(wsen[2], wsen[1]);

        // double[] bs = new double[]{tl[0], tl[1], br[0], br[1]};
        // if (bs[2] <= bs[0]) bs[2] += getGlobalPixelMax()[0];

        // return bs;
    }

    /**
     * Splits view into smaller blocks, each with a maximum side-
     * length of a certain length. If "enough map is left", the
     * maximum side length becomes the side length of the block.
     * That means, only blocks in last row or column can be smaller
     * than the maximum side length.
     *
     * @param l Maximum side length in pixels. Note that this is
     * dependent on the pixel dentity.
     * @return A 2d-layout of views.
     */
    public MapImageView[][] split(int l) {
        int rows = this.height / l;
        int cols = this.width / l;
        if (this.height % l != 0) rows += 1;
        if (this.width % l != 0) cols += 1;

        MapImageView[][] vs = new MapImageView[rows][cols];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int x0 = c * l;
                int y0 = r * l;
                int x1 = Math.min(x0 + l, this.width);
                int y1 = Math.min(y0 + l, this.height);
                double xMid = (x0 + x1) / 2d;
                double yMid = (y0 + y1) / 2d;
                double[] latlon = this.getGeoCoordinates(xMid, yMid);

                vs[r][c] = new MapImageView(latlon[0], latlon[1], x1-x0, y1-y0, this.zoom, this.x2);
            }
        }

        return vs;
    }

    /**
     * Constructs a new view with extended dims so that all originally
     * cut labels fit, plus an extra margin. Only extends if not
     * going outside bounds. Therefore, the mid-point of the
     * view might change.
     *
     * @return An extended view.
     */
    public MapImageView getExtendedView() {
        int ext = getExtensionTerm();

        double[] midGlob = getPixelCoordinates_global(0, this.lat, this.zoom, this.x2);
        double yGlobTop = midGlob[1] - this.height / 2d;
        double yGlobBot = midGlob[1] + this.height / 2d;
        int[] globMax = getGlobalPixelMax(this.zoom, this.x2);
        double newYGlobTop = Math.max(0, yGlobTop - ext);
        double newYGlobBot = Math.min(globMax[1], yGlobBot + ext);
        double newYGlobMid = (newYGlobTop + newYGlobBot) / 2;

        double lon = this.lon;
        double lat = getGeoCoordinates_global(0, newYGlobMid, this.zoom, this.x2)[1];
        int w = Math.min(globMax[0], this.width + ext*2);
        int h = (int)(newYGlobBot - newYGlobTop);

        return new MapImageView(lon, lat, w, h, this.zoom, this.x2);
    }

    /**
     * @return value ext where [left-ext, top-ext, right+ext, bot+ext]
     * is extended pixel-bounds where originally cut labels fit,
     * plus extra margin (for edge-label detection).
     */
    public int getExtensionTerm() {
        int ext = 100;
        if (this.x2) ext *= 2;

        return ext;
    }

    /**
     * @param bs [xmin ymin xmax ymax] pixel-coordinate points.
     * @return [WSEN]. Might be outside normal longitude-values.
     * W always less than E (no wrapping at 180deg). N/S-bounds
     * apply (not outside +/- ~90).
     */
    public double[] getGeoBounds(double[] bs) {
        double[] ws = getGeoCoordinates(bs[0], bs[3]);
        double[] en = getGeoCoordinates(bs[2], bs[1]);
        double w = ws[0];
        double s = ws[1];
        double e = en[0];
        double n = en[1];
        if (w > e) e += 360;
        return new double[]{w, s, e, n};
    }
    public double[] getGeoBounds(int[] bs) {
        return getGeoBounds(Math2.toDouble(bs));
    }

    /**
     * Turn pixel-coordinates into geo-coordinates. Pixel cordinates
     * are specific to associated map-image (local).
     * (0,0) is top left corner of this image.
     *
     * @param x Local x-pixel.
     * @param y Local y-pixel.
     * @return [lon, lat]]
     */
    public double[] getGeoCoordinates(double x, double y) {
        if (x < 0) x += getGlobalPixelMax()[0];
        if (x > this.width) x -= getGlobalPixelMax()[0];

        double[] globalMid = getPixelCoordinates_global(this.lon, this.lat, this.zoom, this.x2);
        double globalX = globalMid[0] - this.width/2d + x;
        double globalY = globalMid[1] - this.height/2d + y;

        return getGeoCoordinates_global(globalX, globalY, this.zoom, this.x2);
    }
    public double[] getGeoCoordinates(double[] xy) {
        return getGeoCoordinates(xy[0], xy[1]);
    }

    /**
     * Turn geo-coordinates into pixel-coordinates (local to this
     * image). Opposite of getGeoCoordinates().
     *
     * @param lon Longitude.
     * @param lat Latitude.
     * @return Local [x, y]
     */
    public double[] getPixelCoordinates(double lon, double lat) {
        double[] globalMid = getPixelCoordinates_global(this.lon, this.lat, this.zoom, this.x2);
        double tlx = globalMid[0] - this.width/2d;
        double tly = globalMid[1] - this.height/2d;

        double[] globalXY = getPixelCoordinates_global(lon, lat, this.zoom, this.x2);

        double[] p = new double[]{ globalXY[0]-tlx, globalXY[1]-tly };
        if (p[0] < 0) p[0] += getGlobalPixelMax()[0];
        if (p[0] > this.width) p[0] -= getGlobalPixelMax()[0];

        return p;
    }
    public double[] getPixelCoordinates(double[] ll) {
        return getPixelCoordinates(ll[0], ll[1]);
    }

    /**
     * Turn global pixel-coordinates into geo-coordinates. Global
     * pixel-coordinates is defined by a grid covering the whole
     * earth (lat +-85, lon +-180). The granularity of the grid is
     * defined by zoom-level and pixel-density.
     * (0,0) is top left corner i.e lon -180, lat ~85.
     *
     * @param x Global x-pixel.
     * @param y Global y-pixel.
     * @param z Zoom level.
     * @param x2 Double pixel-density.
     * @return [lon, lat]
     */
    public/***/ static double[] getGeoCoordinates_global(double x, double y, int z, boolean x2) {
        int q = DEFAULT_TILE_SIZE;
        if (x2) q *= 2;

        double lon = 2*x*Math.PI / (q*Math.pow(2, z)) - Math.PI;
        double lat = 2*Math.atan(Math.exp(Math.PI - 2*Math.PI*y / (q*Math.pow(2, z)))) - Math.PI/2;

        return new double[]{ Math2.toUnitDegrees(Math.toDegrees(lon)),
                             Math2.toUnitDegrees(Math.toDegrees(lat)) };
    }

    /**
     * Turn geo-coordinates into global pixel-coordinates.
     * Opposite of getGeoCoordinates_global().
     *
     * @param lon Longitude.
     * @param lat Latitude.
     * @param z Zoom level.
     * @param x2 Double pixel-density.
     * @return Global [x, y]
     */
    public/***/ static double[] getPixelCoordinates_global(double lon, double lat, int z, boolean x2) {
        lon = Math.toRadians(Math2.toUnitDegrees(lon));
        lat = Math.toRadians(Math2.toUnitDegrees(lat));

        int q = DEFAULT_TILE_SIZE;
        if (x2) q *= 2;

        double x = q / (2*Math.PI) * Math.pow(2, z) * (lon + Math.PI);
        double y = q / (2*Math.PI) * Math.pow(2, z) * (Math.PI - Math.log(Math.tan(Math.PI/4 + lat/2)));
        return new double[]{ x, y };
    }
    public/***/ double[] getPixelCoordinates_global(double lon, double lat) {
        return getPixelCoordinates_global(lon, lat, this.zoom, this.x2);
    }

    @Override
    public String toString() {
        return
            "lon: " + this.lon + ", " +
            "lat: " + this.lat + ", " +
            "width: " + this.width + ", " +
            "height: " + this.height + ", " +
            "zoom: " + this.zoom;
    }


    //------------------------------------------for testing

    public static boolean x2_ = false;

    public static MapImageView randomize() {
        int minDim = 100;
        int maxDim = 10000;
        int w = Math2.randInt(minDim, maxDim+1);
        int h = Math2.randInt(minDim, maxDim+1);
        double lon = Math2.randDouble(-180, 180);
        double lat = Math2.randDouble(-89, 89);
        int z = Math2.randInt(0, 20+1);

        try {
            return new MapImageView(lon, lat, w, h, z, x2_);
        }
        catch (IllegalArgumentException e) {
            return randomize();
        }
    }

    public static MapImageView world() {
        double w = -180;
        double s = -MapImageView.LATITUDE_BOUND;
        double e = 180;
        double n = MapImageView.LATITUDE_BOUND;
        int z = 3;
        return new MapImageView(w, s, e, n, z, x2_);
    }

    public static MapImageView europe() {
        double w = -18.36914062;
        double s = 32.99023556;
        double e = 56.33789063;
        double n = 60.06484046;
        int z = 5;
        return new MapImageView(w, s, e, n, z, x2_);
    }

    public static MapImageView sweden() {
        double w = 9.887695;
        double s = 55.336956;
        double e = 23.862305;
        double n = 69.446949;
        int z = 5;
        return new MapImageView(w, s, e, n, z, x2_);
    }

    public static MapImageView uppsala() {
        double w = 17.572212;
        double s = 59.784881;
        double e = 17.73529;
        double n = 59.879084;
        int z = 12;
        return new MapImageView(w, s, e, n, z, x2_);
    }

    public static MapImageView luthagen() {
        double w = 17.602767;
        double s = 59.85836;
        double e = 17.649845;
        double n = 59.863962;
        int z = 16;
        return new MapImageView(w, s, e, n, z, x2_);
    }

    public static MapImageView lidingo() {
        double w = 18.08246612548828;
        double s = 59.33564087770051;
        double e = 18.27404022216797;
        double n = 59.39407306645033;
        int z = 14;
        return new MapImageView(w, s, e, n, z, x2_);
    }

    public static MapImageView rudboda() {
        double w = 18.15;
        double s = 59.372;
        double e = 18.19;
        double n = 59.383;
        int z = 17;
        return new MapImageView(w, s, e, n, z, x2_);
    }

    public static MapImageView mefjard() {
        double w = 18.460774;
        double s = 58.958251;
        double e = 18.619389;
        double n = 59.080544;
        int z = 13;
        return new MapImageView(w, s, e, n, z, x2_);
    }

    public static MapImageView lonEdge() {
        double w = 177;
        double s = 54;
        double e = -168;
        double n = 66;
        int z = 3;
        return new MapImageView(w, s, e, n, z, x2_);
    }

    public static MapImageView newYork() {
        double w = -74.016259;
        double s = 40.717569;
        double e = -73.972399;
        double n = 40.737473;
        int z = 15;
        return new MapImageView(w, s, e, n, z, x2_);
    }
}
