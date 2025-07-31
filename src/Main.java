
public class Main {
    public static void main(String[] args) {

        Jmap<Integer, Integer> map = new Jmap<>();
        map.put(1, 10);
        map.put(2, 20);
        map.put(3, 30);
        map.put(13, 40);
        map.put(23, 50);
        map.put(53, 1030);
        map.put(33, 220);
        map.put(63, 550);
        map.put(93, 210);
        map.put(112, 193);
        map.put(29, 124);
        map.put(193, 190);
        map.put(193, 999);
        System.out.println(map);

        Jnode<Integer,Integer> getValue = map.get(123923);
        System.out.println(getValue);
    }
}