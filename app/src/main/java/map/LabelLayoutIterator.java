package map;

import java.awt.Color;
import java.util.LinkedList;
import java.util.Arrays;
import java.awt.Color;


/**
 * Iterator that finds labels (clusters of boxes) in a box-image
 * and returns each label's layout. Scans through a box-image,
 * finds a box-point, expands it into a label-layout.
 * Returns good and bad (e.g clipped, half outside of view) labels.
 *
 * A box-image is an image where only labels are showing and instead
 * of letters in the labels, there are boxes. Each box is formed like
 * the symbol [ with the opening towards right -to provide direction.
 * The space-character is also represented as a box.
 * The box is sized and positioned so that corresponding letter
 * precisely fits inside. If the character is very short/thin the
 * box is given a min-width/height (which doesn't affect the
 * letter-spacing/box-spacing relationship).
 *
 * Note: the underlying font must comply with box-image constraints -
 * the font should be mono-spaced.
 *
 * The boxes (letters) form groups:
 *  -Rows of horizontally adjacent boxes.
 *  -Labels (multiple rows) separated by surrounding space.
 *
 * More underlying properties:
 *  -Labels are located anywhere in the image, at any rotation.
 *  -The baseline of a row might not be a straight line - letters
 *   are rotated accordingly folloing the row. Maximum angle change
 *   between adjacent characters is 15deg.
 *  -Labels with multiple rows has no rotation and a straight base line.
 *
 * This class represents a box-image using a 2D boolean array (a "map").
 * If map[x][y] is True, this is a box-point.
 *
 * @pre No letter-boxes touch!
 * @pre Horizontal space between boxes of same row is always less
 * than the width of any box in the label. (...!)
 * @pre Hight of a box is always shorter than 2*height of any box in
 * same label (i.e same font-size) (and higher than 0.5*any box
 * in label).
 * @pre Rotation change between adjacent letters in a label is <=15deg.
 * @pre Vertical space between boxes of neighboring rows is always
 * less than the heigh of any box in the label. (...!)
 */
public class LabelLayoutIterator {
    /**
     * Pixels alpha-value-threshold where over means box-point. */
    public/***/ static final int DEFAULT_ALPHA_THRESHOLD = 100;

    /**
     * Max search length from left/right edge of a box to
     * a neighbor-box (same label) is length(box)*this. */
    private static final double BOX_SEARCH_LENGTH_FACTOR = 1.5;

    /**
     * Max search length from top/botten edge of a box to a
     * neighbor-row-box (same label) is height(box)*this. */
    private static final double ROW_SEARCH_LENGTH_FACTOR = 1.5;

    /**
     * sb/hb >= this, where sb is shortest, tb tallest box, in
     * any label. */
    private static final double MAX_BOX_HEIGHT_DIFFERENCE_FACTOR = 0.5;

    /**
     * The maximum change in angle between two adjacent boxes. */
    private static final double MAX_ANGLE_CHANGE = 15;

    /**
     * Rows with x-positions matching within this value are
     * considered centered. */
    private static final double CENTERED_LAXNESS = 5;


    /** Start searching for next box-point at this pos in map. */
    public/***/ int startX = 0;
    public/***/ int startY = 0;

    /** Representation of box-image: map[row][column]. */
    public/***/ boolean[][] map;

    /**
     * Constructs the iterator from an rgba-image (box-image).
     * Close to transparent pixels are marked as non-box-point.
     * Lowest alpha value still counted as box-point is the
     * alpha-threshold.
     *
     * @param img A box-image (an rgba-image).
     * @param alphaThreshold Pixels alpha-value-threshold where
     * over means box-point, under means non-box-point.
     *
     * @pre 0 <= alphaThreshold <= 255
     */
    public LabelLayoutIterator(BasicImage img, int alphaThreshold) {
        this.map = new boolean[img.getHeight()][img.getWidth()];

        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                Color c = img.getColor(x, y);
                int alpha = c.getAlpha();
                if (alpha >= alphaThreshold) {
                    this.map[y][x] = true;
                }
                else {
                    this.map[y][x] = false;
                }
            }
        }
    }

    /**
     * Constructor for using default value for alphaThreshold.
     */
    public LabelLayoutIterator(BasicImage img) {
        this(img, DEFAULT_ALPHA_THRESHOLD);
    }

    /**
     * Finds and returns next layout. Starts searching at startX
     * startY, and sets this to found box-point. Removes
     * found label from map so it's not found again.
     *
     * @return Iterator's next label layout. NULL if no more.
     */
    public LabelLayout next() {
        int[] p = findBoxPoint(this.startX, this.startY);
        if (p == null) return null;

        this.startX = p[0];
        this.startY = p[1];

        LabelLayout lay = expandToLabelLayout(p);
        if (lay == null) {
            expandAndRemove(p);
            return next();
        }

        removeLabel(lay);
        if (!isEdgeLabel(lay)) return lay;
        else return next();
    }

    /**
     * Scans through the map, looking for a box-point.
     *
     * @return A box-point in the map, or NULL if no more
     * box-points, as [x,y].
     */
    public/***/ int[] findBoxPoint(int startX, int startY) {
        for (int y = startY; y < map.length; y++) {
            for (int x = startX; x < map[y].length; x++) {
                if (isBoxPoint(x, y))
                    return new int[]{x, y};
            }
        }
        return null;
    }

    /**
     * Expands box-point to label-layout.
     *
     * 1.Box-point expands to box.
     * 2.Looks left/right for more boxes -> row.
     * 3.Looks up/down for more rows -> layout.
     *
     * @param bp Box-point.
     * @return The layout of the label that contains bp, or NULL
     * if bp wont expand to a box.
     */
    private LabelLayout expandToLabelLayout(int[] bp) {
        Box b = expandToBox(bp);
        if (b == null) return null;

        LinkedList<Box> row = expandToRow(b);
        LabelLayout lay = new LabelLayout(row);

        if (lay.hasObviousRotation()) return lay;

        boolean up = true;
        addRows(up, row, lay);
        up = false;
        addRows(up, row, lay);

        return lay;
    }

    /**
     * Expand a box-point to a box and the box to a horizontal
     * row of boxes.
     *
     * Looks left/right to expanded box (at certain distance..).
     * Stops looking when finding:
     *  - Nothing.
     *  - A box at wrong size/rotation.
     *  - The box-image-edge.
     *  - An edge-box (expanded to null).
     *
     * @return A row in the map (i.e horizontaly adjacent boxes).
     * As a minumum [startBox].
     */
    public/***/ LinkedList<Box> expandToRow(Box startBox) {
        LinkedList<Box> row = new LinkedList<Box>();
        row.add(startBox);

        boolean left = true;
        addBoxes(left, startBox, row);
        left = false;
        addBoxes(left, startBox, row);

        return row;
    }

    /**
     * Adds, to a LabelLayout, all rows above or below some
     * start-row in same label in the map. Done when nothing
     * neighbor-row-like there.
     *
     * @param up If true, adds rows above startRow, otherwise below.
     * @param startRow Start-row.
     * @param lay Accumulator for the new rows.
     * @return The provided label-layout, prepended/appended
     * with all rows above/below in same label as start-row.
     *
     * @pre startRow has no rotation and a straight base-line.
     */
    public/***/ LabelLayout addRows(boolean up, LinkedList<Box> startRow, LabelLayout lay) {
        LinkedList<Box> neigh = findNeighborRow(up, startRow);
        if (neigh == null) return lay;

        if (up) lay.addRowFirst(neigh);
        else lay.addRowLast(neigh);

        return addRows(up, neigh, lay);
    }

    /**
     * Removes a label from the map, i.e deactivates all its
     * points. Goes through the label-layout and deactivates every
     * point of every letter-box.
     *
     * @param lay LabelLayout for the label to be removed.
     */
    public/***/ void removeLabel(LabelLayout lay) {
        for (Box b : lay.getBoxes()) {
            int[] bp = getInsideBoxPoint(b);
            expandAndRemove(bp);
        }
    }

    // /**
    //  * Removes null-boxes.
    //  *
    //  * @param lay A box in the layout may be null, meaning something
    //  * is here but clipped because outside of box-image.
    //  * Only first/last box of first/last row may be null.
    //  */
    // private void removeNullBoxes(LinkedList<LinkedList<Box>> lay) {
    //     LinkedList<Box> firstR = lay.getFirst();
    //     LinkedList<Box> lastR = lay.getLast();

    //     if (firstR.getFirst() == null) {
    //         boolean left = true;
    //         expandAndRemove(findNeighborBoxPoint(left, firstR.get(1)));
    //     }
    //     if (lastR.getFirst() == null) {
    //         boolean left = true;
    //         expandAndRemove(findNeighborBoxPoint(left, lastR.get(1)));
    //     }
    //     if (firstR.getLast() == null) {
    //         boolean left = false;
    //         expandAndRemove(findNeighborBoxPoint(left, firstR.get(firstR.size()-2)));
    //     }
    //     if (lastR.getLast() == null) {
    //         boolean left = false;
    //         expandAndRemove(findNeighborBoxPoint(left, lastR.get(lastR.size()-2)));
    //     }
    // }

    /**
     * Expands point to all connecting box-points, and removes them.
     */
    public/***/ void expandAndRemove(int[] p) {
        LinkedList<int[]> ps = expandToBoxPoints(p);
        for (int[] q : ps) map[ q[1] ][ q[0] ] = false;
    }

    // /**
    //  * @param lay Layout with possible null-boxes at end/beginning of
    //  * first/last row.
    //  * @return True if layout has no null-boxes (clipped and unknown).
    //  */
    // private boolean layoutIsComplete(LinkedList<LinkedList<Box>> lay) {
    //     LinkedList<Box> firstR = lay.getFirst();
    //     LinkedList<Box> lastR = lay.getLast();

    //     return
    //         firstR.getFirst() != null &&
    //         firstR.getLast() != null &&
    //         lastR.getFirst() != null &&
    //         lastR.getLast() != null;
    // }

    /**
     * @param b A box extracted from this box-image.
     * @return A box-point inside this box.
     *
     * @pre b a box that contains box-points.
     */
    private int[] getInsideBoxPoint(Box b) {
        int[] start = Math2.toInt(b.getTopLeft());
        int[] end = Math2.toInt(b.getBottomRight());
        PixelWalk pw = new PixelWalk(start, end);

        int[] p = null;
        while ((p = pw.next()) != null) {
            if (isBoxPoint(p)) return p;
        }

        throw new RuntimeException();
    }

    /**
     * Adds, to a list, all boxes to the left or right of some
     * start-box, in same row of some label in the map. Done when
     * nothing neighbor-box-like there.
     *
     * @param left If true, adds boxes to the left of startBox,
     * otherwise to the right.
     * @param start Start-box.
     * @param bs Accumulator for the new boxes.
     * @return The provided box-array (bs) prepended/appended
     * with all boxes to left/right of that box (in same label).
     */
    public/***/ LinkedList<Box> addBoxes(boolean left, Box start, LinkedList<Box> bs) {
        Box neigh = findNeighborBox(left, start);
        if (neigh == null) return bs;

        if (left) bs.addFirst(neigh);
        else bs.addLast(neigh);

        return addBoxes(left, neigh, bs);
    }

    /**
     * Finds a neighbor-row of a start row.
     * Looks either up or down from the start row. A neighbor-row
     * is an adjacent row in same label as the start-row.
     *
     * Conditions for beeing a neighbor-row:
     *  -No rotation (like start-row).
     *  -In close proximity (up/down) to start-row.
     *  -Generally same line-height as start-row.
     *  -Centered in relation to start-row.
     *
     * @param up Search up, otherwise down.
     * @param sr Start-row.
     * @return A neighbor-row either up or down of startRow,
     * or NULL if such a row doesn't exist.
     *
     * @pre sr has no obvious rotation and a straight baseline.
     */
    public/***/ LinkedList<Box> findNeighborRow(boolean up, LinkedList<Box> sr) {
        if (new LabelLayout(sr).hasObviousRotation())
            throw new RuntimeException();

        int[] np = findPotentialNeighborRowPoint(up, sr);
        Box nb = expandToBox(np);
        if (nb == null) return null;

        LinkedList<Box> nr = expandToRow(nb);
        LabelLayout sl = new LabelLayout(sr);
        LabelLayout nl = new LabelLayout(nr);

        if (nl.hasObviousRotation()) return null;
        if (sl.getShortestBoxHeight() / nl.getTallestBoxHeight() < MAX_BOX_HEIGHT_DIFFERENCE_FACTOR) return null;
        if (nl.getShortestBoxHeight() / sl.getTallestBoxHeight() < MAX_BOX_HEIGHT_DIFFERENCE_FACTOR) return null;
        if (!rowsCentered(sr, nr)) return null;

        return nr;
    }

    /**
     * @param up If true searches up, otherwise down.
     * @param sr Start-row.
     * @return Point in potential neighbor-row.
     */
    private int[] findPotentialNeighborRowPoint(boolean up, LinkedList<Box> sr) {
        double dist = new LabelLayout(sr).getAverageBoxHeight() * ROW_SEARCH_LENGTH_FACTOR;
        Box midB = sr.get(sr.size() / 2);

        double[] start0 = midB.getTopLeft();
        double[] start1 = midB.getTopRight();
        double[] end0 = Math2.step(start0, new double[]{0,-1}, dist);
        double[] end1 = Math2.step(start1, new double[]{0,-1}, dist);
        if (!up) {
            start0 = midB.getBottomLeft();
            start1 = midB.getBottomRight();
            end0 = Math2.step(start0, new double[]{0,1}, dist);
            end1 = Math2.step(start0, new double[]{0,1}, dist);
        }

        int[] np = findBoxPointOnPath(start0, end0);
        if (np == null)
            np = findBoxPointOnPath(start1, end1);

        return np;
    }

    /**
     * @return First box-point you come across when walking from start
     * till end, or NULL if none/edge.
     */
    private int[] findBoxPointOnPath(int[] start, int[] end) {
        PixelWalk pw = new PixelWalk(start, end);

        int[] p;
        while ((p=pw.next()) != null) {
            if (isBoxPoint(p)) return p;
        }

        return null;
    }
    private int[] findBoxPointOnPath(double[] start, double[] end) {
        return findBoxPointOnPath(Math2.toInt(start), Math2.toInt(end));
    }

    /**
     * @return True if rows are centered above/below eachother.
     * @pre Non-rotated rows.
     */
    private boolean rowsCentered(LinkedList<Box> r0, LinkedList<Box> r1) {
        double[] bs0 = new LabelLayout(r0).getBounds();
        double[] bs1 = new LabelLayout(r1).getBounds();

        double deltaLeft = bs0[0] - bs1[0];
        double deltaRight = bs1[2] - bs0[2];

        return Math2.same(deltaLeft, deltaRight, CENTERED_LAXNESS);
    }

    /**
     * Finds a neighbor-box to a start-box.
     * Looks to either left or right side of the start box.
     * A neighbor-box is an adjacent box in same label.
     *
     * Conditions for beeing a neighboring box:
     *  -In close proximity to left/right of start-box.
     *  -At about same height as start-box.
     *  -At about the same rotation as start-box.
     *
     * @param left Search left, otherwise right.
     * @param sb Start-box.
     * @return A left/right neighbor-box, or NULL if such a
     * neighbor-box doesn't exist.
     */
    public Box findNeighborBox(boolean left, Box sb) {
        int[] np = findPotentialNeighborBoxPoint(left, sb);
        if (np == null) return null;

        Box nb = expandToBox(np);
        if (nb == null) return null;

        if (nb.getHeight() / sb.getHeight() < MAX_BOX_HEIGHT_DIFFERENCE_FACTOR)
            return null;
        if (sb.getHeight() / nb.getHeight() < MAX_BOX_HEIGHT_DIFFERENCE_FACTOR)
            return null;
        if (Math2.angleDiff(sb.getRotation(), nb.getRotation()) > MAX_ANGLE_CHANGE)
            return null;

        return nb;
    }

    /**
     * Finds a point in a neighboring box, or NULL if no neighbor
     * there. Looks either left or right.
     *
     * @param left Search left, otherwise right.
     * @param sb Start-box.
     */
    public/***/ int[] findPotentialNeighborBoxPoint(boolean left, Box sb) {
        int[] start, end;
        double maxD = sb.getWidth() * 1.1;

        //first try: mid
        if (left) {
            start = Math2.toInt(sb.getLeftMid());
            end = Math2.step(start, sb.getDirVector(), -maxD);
        }
        else {
            start = Math2.toInt(sb.getRightMid());
            end = Math2.step(start, sb.getDirVector(), maxD);
        }
        int[] np = findBoxPointOnPath(start, end);
        if (np != null) return np;

        //one more try: top
        if (left) {
            start = Math2.toInt(sb.getTopLeft());
            end = Math2.step(start, sb.getDirVector(), -maxD);
        }
        else {
            start = Math2.toInt(sb.getTopRight());
            end = Math2.step(start, sb.getDirVector(), maxD);
        }
        np = findBoxPointOnPath(start, end);
        if (np != null) return np;

        //last try: bottom
        if (left) {
            start = Math2.toInt(sb.getBottomLeft());
            end = Math2.step(start, sb.getDirVector(), -maxD);
        }
        else {
            start = Math2.toInt(sb.getBottomRight());
            end = Math2.step(start, sb.getDirVector(), maxD);
        }
        return findBoxPointOnPath(start, end);
    }

    /**
     * Finds all boxed-points connected to the start-point and
     * created a box from these points.
     *
     * @param start Start-point.
     * @return A box encapsulating start-point and all connceted
     * box-points, or NULL if resulting box is outside or on the
     * box-image edge.
     *
     * @pre start is a box-point in the map.
     */
    public/***/ Box expandToBox(int[] start) {
        LinkedList<int[]> ps = expandToBoxPoints(start);
        if (containsEdgePoint(ps)) return null;

        Box b = new Box(ps);
        if (isInside(b)) return b;
        else return null;
    }

    /**
     * @param start Start-point.
     * @return All box-points connected to start (including start).
     * Empty list if start not a box-point.
     */
    public/***/ LinkedList<int[]> expandToBoxPoints(int[] start) {
        if (!isBoxPoint(start)) return new LinkedList<int[]>();

        LinkedList<int[]> open = new LinkedList<int[]>();
        LinkedList<int[]> closed = new LinkedList<int[]>();
        open.add(start);

        while (open.size() > 0) {
            int[] current = open.removeFirst();
            LinkedList<int[]> ns = getBoxPointNeighbors(current);
            LinkedList<int[]> uns = Math2.getUniquePoints(ns, open, closed);
            open.addAll(uns);
            closed.add(current);
        }
        return closed;
    }

    /**
     * @return The box-point neighbors (left/up/right/down).
     * @pre p is box-point.
     */
    public/***/ LinkedList<int[]> getBoxPointNeighbors(int[] p) {
        if (!isBoxPoint(p)) throw new RuntimeException();

        int[] left = new int[]{ p[0]-1, p[1] };
        int[] up = new int[]{ p[0], p[1]-1 };
        int[] right = new int[]{ p[0]+1, p[1] };
        int[] down = new int[]{ p[0], p[1]+1 };

        LinkedList<int[]> ns = new LinkedList<int[]>();
        if (isBoxPoint(left)) ns.add(left);
        if (isBoxPoint(up)) ns.add(up);
        if (isBoxPoint(right)) ns.add(right);
        if (isBoxPoint(down)) ns.add(down);
        return ns;
    }

    /**
     * @return True if any point in ps is an edge-point.
     */
    public/***/ boolean containsEdgePoint(LinkedList<int[]> ps) {
        for (int[] p : ps) {
            if (isEdgePoint(p)) return true;
        }
        return false;
    }

    /**
     * @return True if p is an edge-point, i.e a point at the
     * edge of the box-image.
     */
    public/***/ boolean isEdgePoint(int[] p) {
        return
            p[0] == 0 ||
            p[0] == map[0].length-1 ||
            p[1] == 0 ||
            p[1] == map.length-1;
    }
    private boolean isEdgePoint(double[] p) {
        return isEdgePoint(Math2.toInt(p));
    }

    /**
     * @return True if box is inside box-image.
     */
    public/***/ boolean isInside(Box b) {
        int[][] cs = Math2.toInt(b.getCorners());
        return
            isInside(cs[0]) && isInside(cs[1]) &&
            isInside(cs[2]) && isInside(cs[3]);
    }

    /**
     * @return True if point p is inside box-image.
     */
    public/***/ boolean isInside(int[] p) {
        return
            p[0] >= 0 && p[0] < map[0].length &&
            p[1] >= 0 && p[1] < map.length;
    }

    /**
     * @return True if [x,y] is a box-point.
     */
    public/***/ boolean isBoxPoint(int x, int y) {
        if (y < 0 || y >= map.length) return false;
        if (x < 0 || x >= map[0].length) return false;

        return map[y][x];
    }
    public/***/ boolean isBoxPoint(int[] p) {
        return isBoxPoint(p[0], p[1]);
    }

    /**
     * @return True if map is empty (all points false).
     */
    private boolean mapIsEmpty() {
        for (boolean[] row : map) {
            for (boolean p : row) {
                if (p == true) return false;
            }
        }
        return true;
    }

    /**
     * @return True if layout might continue outside of box-image.
     */
    private boolean isEdgeLabel(LabelLayout l) {
        return
            isEdgeBox(l.getBox(0,0)) ||
            isEdgeBox(l.getBox(0,-1)) ||
            isEdgeBox(l.getBox(-1,0)) ||
            isEdgeBox(l.getBox(-1,-1));
    }

    /**
     * @return True if box might have a neighbor outside of box-image
     * (box-neighbor or row-neighbor).
     */
    private boolean isEdgeBox(Box b) {
        double hl = b.getWidth() * BOX_SEARCH_LENGTH_FACTOR;
        double vl = b.getHeight() * ROW_SEARCH_LENGTH_FACTOR;
        double[] dv = b.getDirVector();
        double[] ov = b.getOrtoDirVector();

        return
            isEdgePoint(Math2.step(b.getTopLeft(), dv, -hl)) ||
            isEdgePoint(Math2.step(b.getTopLeft(), ov, -vl)) ||
            isEdgePoint(Math2.step(b.getTopRight(), dv, hl)) ||
            isEdgePoint(Math2.step(b.getTopRight(), ov, -vl)) ||
            isEdgePoint(Math2.step(b.getBottomRight(), dv, hl)) ||
            isEdgePoint(Math2.step(b.getBottomRight(), ov, vl)) ||
            isEdgePoint(Math2.step(b.getBottomLeft(), dv, -hl)) ||
            isEdgePoint(Math2.step(b.getBottomLeft(), ov, vl));
    }

    //*********************************FOR TESTING

    public BasicImage toImg() {
        BasicImage img = new BasicImage(map[0].length, map.length);

        for (int y = 0; y < map.length; y++) {
            for (int x = 0; x < map[y].length; x++) {
                if (map[y][x])
                    img.setColor(x, y, Color.BLACK);
            }
        }
        return img;
    }

    /**
     * Color points in ps black.
     */
    public static BasicImage toImg(LinkedList<int[]> ps) {
        int xmin = Integer.MAX_VALUE;
        int ymin = Integer.MAX_VALUE;
        int xmax = 0;
        int ymax = 0;
        for (int[] p : ps) {
            if (p[0] < xmin) xmin = p[0];
            if (p[1] < ymin) ymin = p[1];
            if (p[0] > xmax) xmax = p[0];
            if (p[1] > ymax) ymax = p[1];
        }

        BasicImage img = new BasicImage(xmax+1-xmin, ymax+1-ymin);
        for (int[] p : ps)
            img.setColor(p[0]-xmin, p[1]-ymin, Color.BLUE);

        return img;
    }
}
