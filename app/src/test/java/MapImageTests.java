import org.junit.*;
import static org.junit.Assert.*;
import map.*;
import java.io.IOException;

public class MapImageTests {
    Language lang = Language.LOCAL;
    MapImageView view = MapImageView.luthagen();

    @Test
    public void construction() throws IOException {
        // constructAndDump(view, "lidingo");
    }

    /** Set MapRequest.FETCH = true */
    // @Test
    // public void fuzz() throws IOException {
    //     while (true) {
    //         MapImageView v = MapImageView.randomize();
    //         System.out.println(v);
    //         constructAndDump(v);
    //         System.out.println("\n");
    //     }
    // }

    public/***/ void constructAndDump(MapImageView v, String name) throws IOException {
        MapImage mimg;

        if (MapRequest.FETCH) {
            mimg = new MapImage(v, lang);
        }
        else {
            MapRequest.ViewAndImgs vis = new MapRequest.ViewAndImgs(name, v);
            mimg = new MapImage(vis.imgs, vis.view, lang);
        }

        BasicImage dump = mimg.getImg();

        dump.save("test_" + name + "_before.png");
        for (MapObject mob : mimg.getObjects()) {
            dump.drawMapObject(mob);
        }
        dump.save("test_" + name + "_after.png");
    }
    public/***/ void constructAndDump(MapImageView v) throws IOException {
        constructAndDump(v, "test_" + v.toString());
    }
}
