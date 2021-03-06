import org.junit.Test;
import static org.junit.Assert.*;
import map.*;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Arrays;

public class BasicImageTests {

    // @Test
    // public void extractElement() {
    //     BasicImage img = new BasicImage(100, 100);
    //     Box box = new Box(new double[]{10, 40}, new double[]{50, 10}, 60);
    //     img.drawBox(box);
    //     img.save("1before.png");
    //     BasicImage elem = img.extractElement(box);
    //     elem.save("1extracted.png");

    //     // img = new BasicImage(100, 100);
    //     // box = new Box(new double[]{30, 10}, new double[]{60, 20}, 60);
    //     // img.drawBox(box);
    //     // img.save("2before.png");
    //     // elem = img.extractElement(box);
    //     // elem.save("2extracted.png");

    //     // img = new BasicImage(100, 100);
    //     // box = new Box(new double[]{30, 80}, new double[]{10, 50}, 60);
    //     // img.drawBox(box);
    //     // img.save("3before.png");
    //     // elem = img.extractElement(box);
    //     // elem.save("3extracted.png");
    // }

    // @Test
    // public void extractElement_real() {
    //     BasicImage boxImg = BasicImage.load("../test_europe_box.png");
    //     BasicImage labelImg = BasicImage.load("../test_europe_label.png");

    //     LabelLayoutIterator iter = new LabelLayoutIterator(boxImg);
    //     LabelLayout l = iter.expandToLabelLayout(new int[]{970,1393});

    //     int PADDING = 5;
    //     BasicImage line = labelImg.extractLabel(l, PADDING);
    //     line.save("extractElement_real.png");
    // }

    @Test
    public void concatenateImages_2x2Layout_squares() {
        int w = 10;
        int h = 10;
        BasicImage img1 = new BasicImage(w, h);
        img1.color(Color.RED);
        BasicImage img2 = new BasicImage(w, h);
        img2.color(Color.GREEN);
        BasicImage img3 = new BasicImage(w, h);
        img3.color(Color.BLUE);
        BasicImage img4 = new BasicImage(w, h);
        img4.color(Color.YELLOW);

        BasicImage[][] imgs = new BasicImage[][]{{img1, img2}, {img3, img4}};
        BasicImage img = BasicImage.concatenateImages(imgs);
        assertEquals(img.getWidth(), 20);
        assertEquals(img.getHeight(), 20);

        //img.save("test_concatenateImages_2x2Layout_squares.png");
    }
}
