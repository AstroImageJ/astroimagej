package nom.tam.image.tile.operation;

import org.junit.Assert;
import org.junit.Test;

public class TileAreaTest {

    @Test
    public void testIntersect() {
        TileArea middle = new TileArea().start(140, 140).end(160, 160);

        Assert.assertTrue(new TileArea().start(0, 150).end(300, 165).intersects(middle));
        Assert.assertFalse(new TileArea().start(0, 100).end(300, 115).intersects(middle));
        Assert.assertFalse(new TileArea().start(15, 0).end(30, 300).intersects(middle));
        Assert.assertFalse(new TileArea().start(170, 0).end(185, 300).intersects(middle));
        Assert.assertFalse(new TileArea().start(0, 170).end(300, 175).intersects(middle));

    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testIntersectException() throws Exception {
        TileArea middle = new TileArea().start(140, 140).end(160, 160);
        middle.intersects(new TileArea().start(2, 3, 4));
    }
    
    @Test
    public void tileAreaSubsizeTest() throws Exception {
        TileArea area = new TileArea().start(2, 3, 4).size(5);
        Assert.assertTrue(area.intersects(new TileArea().start(6, 3, 4).size(1)));
        Assert.assertFalse(area.intersects(new TileArea().start(7, 3, 4).size(1)));
        Assert.assertFalse(area.intersects(new TileArea().start(6, 4, 4).size(1)));
        Assert.assertFalse(area.intersects(new TileArea().start(6, 3, 5).size(1)));
    }
    @Test
    public void emptyTileTest() throws Exception {
        Assert.assertEquals(0, new TileArea().dimension());
    }
    
    
}
