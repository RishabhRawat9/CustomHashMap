
import java.util.Random;

public class Main {
    public static void main(String[] args) {

        Jmap<Integer, Integer> map = new Jmap<>();
        Random r = new Random(42);
        for (int i = 0; i < 12; i++) {
            int key = r.nextInt(10000);
            map.put(key, i);
        }

        map.put(13, 133);
        System.out.println(map.get(13));
        System.out.println(map);
        map.remove(13);
        System.out.println(map.get(13));



    }
}