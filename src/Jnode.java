import org.w3c.dom.Node;

public class Jnode <K, V>{
    public int hash;
    public K key;
    public V value;
    public Jnode<K,V> next; //coz in the bucket we store nodes and in case of collision one bucket can have multiple nodes so we store all of them using a linkedlist;

    public Jnode(int hashIndex, K key, V value){
        this.hash = hashIndex;
        this.key = key;
        this.value = value;
        this.next = null;
    }

    @Override
    public String toString(){

        String content = String.format("(%s, %s)", key, value);
        return content;

    }



}
