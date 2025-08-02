import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JmapTest {

    @Test
    public void testPutAndGet() {
        Jmap<Integer, Integer> map = new Jmap<>();
        map.put(1, 10);
        map.put(2, 20);

        assertEquals(10, map.get(1).value);
        assertEquals(20, map.get(2).value);
    }

    @Test
    public void testUpdateValue() {
        Jmap<Integer, Integer> map = new Jmap<>();
        map.put(1, 10);
        map.put(1, 99);

        assertEquals(99, map.get(1).value);
    }


    @Test
    public void testResize() {
        Jmap<Integer,Integer> map = new Jmap<>();

        for (int i = 0; i < 100; i++) {
            map.put(i, i * 10);
        }

        for (int i = 0; i < 100; i++) {
            assertEquals(i * 10, map.get(i).value);
        }
    }
}
