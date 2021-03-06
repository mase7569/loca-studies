import org.junit.Test;
import static org.junit.Assert.*;
import map.*;

public class PixelWalkTests {

    @Test
    public void rightWalk() {
        PixelWalk pw = new PixelWalk(0, 0, 3, 0);

        int[] p0 = pw.next();
        assertEquals(0, p0[0]);
        assertEquals(0, p0[1]);

        int[] p1 = pw.next();
        assertEquals(1, p1[0]);
        assertEquals(0, p1[1]);

        int[] p2 = pw.next();
        assertEquals(2, p2[0]);
        assertEquals(0, p2[1]);

        int[] p3 = pw.next();
        assertEquals(3, p3[0]);
        assertEquals(0, p3[1]);

        assertEquals(null, pw.next());
    }

    @Test
    public void leftWalk() {
        PixelWalk pw = new PixelWalk(3, 0, 0, 0);

        int[] p0 = pw.next();
        assertEquals(3, p0[0]);
        assertEquals(0, p0[1]);

        int[] p1 = pw.next();
        assertEquals(2, p1[0]);
        assertEquals(0, p1[1]);

        int[] p2 = pw.next();
        assertEquals(1, p2[0]);
        assertEquals(0, p2[1]);

        int[] p3 = pw.next();
        assertEquals(0, p3[0]);
        assertEquals(0, p3[1]);

        assertEquals(null, pw.next());
    }

    @Test
    public void upWalk() {
        PixelWalk pw = new PixelWalk(0, 3, 0, 0);

        int[] p0 = pw.next();
        assertEquals(0, p0[0]);
        assertEquals(3, p0[1]);

        int[] p1 = pw.next();
        assertEquals(0, p1[0]);
        assertEquals(2, p1[1]);

        int[] p2 = pw.next();
        assertEquals(0, p2[0]);
        assertEquals(1, p2[1]);

        int[] p3 = pw.next();
        assertEquals(0, p3[0]);
        assertEquals(0, p3[1]);

        assertEquals(null, pw.next());
    }

    @Test
    public void downWalk() {
        PixelWalk pw = new PixelWalk(0, 0, 0, 3);

        int[] p0 = pw.next();
        assertEquals(0, p0[0]);
        assertEquals(0, p0[1]);

        int[] p1 = pw.next();
        assertEquals(0, p1[0]);
        assertEquals(1, p1[1]);

        int[] p2 = pw.next();
        assertEquals(0, p2[0]);
        assertEquals(2, p2[1]);

        int[] p3 = pw.next();
        assertEquals(0, p3[0]);
        assertEquals(3, p3[1]);

        assertEquals(null, pw.next());
    }

    @Test
    public void rightUpWalk() {
        PixelWalk pw = new PixelWalk(0, 3, 3, 0);

        int[] p0 = pw.next();
        assertEquals(0, p0[0]);
        assertEquals(3, p0[1]);

        int[] p1 = pw.next();
        assertEquals(1, p1[0]);
        assertEquals(2, p1[1]);

        int[] p2 = pw.next();
        assertEquals(2, p2[0]);
        assertEquals(1, p2[1]);

        int[] p3 = pw.next();
        assertEquals(3, p3[0]);
        assertEquals(0, p3[1]);

        assertEquals(null, pw.next());
    }

    @Test
    public void rightDownWalk() {
        PixelWalk pw = new PixelWalk(0, 0, 3, 3);

        int[] p0 = pw.next();
        assertEquals(0, p0[0]);
        assertEquals(0, p0[1]);

        int[] p1 = pw.next();
        assertEquals(1, p1[0]);
        assertEquals(1, p1[1]);

        int[] p2 = pw.next();
        assertEquals(2, p2[0]);
        assertEquals(2, p2[1]);

        int[] p3 = pw.next();
        assertEquals(3, p3[0]);
        assertEquals(3, p3[1]);

        assertEquals(null, pw.next());
    }

    @Test
    public void leftUpWalk() {
        PixelWalk pw = new PixelWalk(3, 3, 0, 0);

        int[] p0 = pw.next();
        assertEquals(3, p0[0]);
        assertEquals(3, p0[1]);

        int[] p1 = pw.next();
        assertEquals(2, p1[0]);
        assertEquals(2, p1[1]);

        int[] p2 = pw.next();
        assertEquals(1, p2[0]);
        assertEquals(1, p2[1]);

        int[] p3 = pw.next();
        assertEquals(0, p3[0]);
        assertEquals(0, p3[1]);

        assertEquals(null, pw.next());
    }

    @Test
    public void leftDownWalk() {
        PixelWalk pw = new PixelWalk(3, 0, 0, 3);

        int[] p0 = pw.next();
        assertEquals(3, p0[0]);
        assertEquals(0, p0[1]);

        int[] p1 = pw.next();
        assertEquals(2, p1[0]);
        assertEquals(1, p1[1]);

        int[] p2 = pw.next();
        assertEquals(1, p2[0]);
        assertEquals(2, p2[1]);

        int[] p3 = pw.next();
        assertEquals(0, p3[0]);
        assertEquals(3, p3[1]);

        assertEquals(null, pw.next());
    }
}
