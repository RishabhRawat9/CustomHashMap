
import java.util.Random;

public class Main {
    public static void main(String[] args) {

        Jmap<Integer, Integer> map = new Jmap<>();
        Random r = new Random(42);
        for (int i = 0; i < 50; i++) {
            int key = r.nextInt(10000);
            map.put(key, i);
        }

        System.out.println(map);

        Jnode<Integer,Integer> getValue = map.get(1911);
        System.out.println(getValue);
    }
}