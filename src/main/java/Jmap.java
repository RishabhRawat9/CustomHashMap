import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Jmap<K, V> {
    private Jnode<K, V>[] table;
    private final AtomicInteger size = new AtomicInteger(16);
    private float resize_threshold = 0.85f;//so when 85% of the capacity is filled i resize the thing right;
    private Object[] bucketLocks;

    private final AtomicBoolean isResizing = new AtomicBoolean(false);

    public static volatile AtomicInteger node_ct = new AtomicInteger(0);

    public static volatile int nodes = 0;

    @SuppressWarnings("unchecked")
    public Jmap() {
        this.table = (Jnode<K, V>[]) new Jnode[size.get()];
        this.bucketLocks = new Object[size.get()];
        for (int j = 0; j < size.get(); j++) {
            bucketLocks[j] = new Object();// for individual locking of the buckets coz if different thread modifying different buckets then i don't need to block the entire map table using the this instance lock will have to use different lock objs;
        }
    }

    @SuppressWarnings("unchecked")
    public Jmap(float threshold) {
        this.table = (Jnode<K, V>[]) new Jnode[size.get()];
        this.resize_threshold = threshold;
        this.bucketLocks = new Object[size.get()];
        for (int j = 0; j < size.get(); j++) {
            bucketLocks[j] = new Object();// for individual locking of the buckets coz if different thread modifying different buckets then i don't need to block the entire map table using the this instance lock will have to use different lock objs;
        }
    }
    //now every time a new node is placed i check if resizing is required or not;

    private int hashFunction(K key, int mapCapacity) {
        //so when a key is provided it returns a hash value;
        int h = key.hashCode();
        h ^= (h >>> 16);  //chatgpt -> good hashfucntion for better spread
        h *= 0x85ebca6b;
        h ^= (h >>> 13);
        h *= 0xc2b2ae35;
        return h & (mapCapacity - 1);
    }

    public void remove(K key) {
        int tableIndex = hashFunction(key, size.get());
        synchronized (bucketLocks[tableIndex]) {
            Jnode<K, V> current = table[tableIndex];
            Jnode<K, V> prev = null;

            if (current == null) {
                throw new IllegalArgumentException("Key not found: " + key);
            }

            while (current != null && !current.key.equals(key)) {
                prev = current;
                current = current.next;
            }

            if (current == null) {
                throw new IllegalArgumentException("Key not found: " + key);
            }

            if (prev == null) {//if it's the first node only;;
                table[tableIndex] = current.next;
            } else {
                prev.next = current.next; // deleted the curr one gc removes it;
            }
            node_ct.getAndDecrement();
        }

    }


    public V get(K key) {//make other threads busy wait if the resizing is going on;

        //ab mujhe is key ka hashvalue chaiye
        int tableIndex = hashFunction(key, size.get());
        //now i can search for this key in my table; //now while this bucket is being used by the get we don't want any other thread to be reading the same bucket;
        synchronized (bucketLocks[tableIndex]) {
            Jnode<K, V> value = table[tableIndex];// this right here is the head of the linked list if muliple nodes reside in the same bucket.
            //now go through the the collided nodes and check their hashcode

            while (value != null) {
                //check each nodes key.equals();
                if (value.key.equals(key)) {
                    return value.value;
                } else {
                    value = value.next;
                }
            }
            return null;
        }

    }

//now for these resize operations i gotta block all other threads and just resize other wise data is corrupted while resizing a put is done or something else then the map is fucked;
    //gotta lock the entire table so that no new operations can't be done until resizing is done;


    public void resize_put(K key, V value, Jnode<K, V>[] newTable) {
        int tableIndex = hashFunction(key, newTable.length);
        Jnode<K, V> node = new Jnode<>(tableIndex, key, value);
        if (newTable[tableIndex] == null) {
            newTable[tableIndex] = node;
        } else {
            Jnode<K, V> currHeadNode = newTable[tableIndex];
            while (currHeadNode != null) {
                if (currHeadNode.next != null) {
                    currHeadNode = currHeadNode.next;
                } else {
                    currHeadNode.next = node;
                    return;
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void resizeJmap() {
        //so a completely new array with twice the size is required now
        int newSize = size.get() * 2;
        Jnode<K, V>[] newTable = (Jnode<K, V>[]) new Jnode[newSize];
        //now all entries need to be traversed and rehashed and put into the new bucket;
        for (int j = 0; j < size.get(); j++) {
            //now for each bucket traverse all the nodes; and place them in new arr;
            Jnode<K, V> currNode = table[j];
            synchronized (bucketLocks[j]) {
                while (currNode != null) {
                    resize_put(currNode.key, currNode.value, newTable);
                    currNode = currNode.next;
                }
            }
        }
        //after resziing is done you also have to allocate more bucket locks for the new arr right/
        this.table = newTable;
        this.size.set(newSize);
        this.bucketLocks = new Object[size.get()];
        for (int j = 0; j < size.get(); j++) {
            bucketLocks[j] = new Object();//stupid had to allocate  more locks aswell ;
        }
    }

    public void put(K key, V value) {

        int tableIndex = hashFunction(key, size.get());
        boolean newNodePlaced = false;
        Jnode<K, V> node = new Jnode<>(tableIndex, key, value);
        synchronized (bucketLocks[tableIndex]) { /// one thread at a time only looks at this bucket all other buckets are free to be modified by other threads;
            System.out.println(Thread.currentThread().getName() + " got lock " + tableIndex);
            if (table[tableIndex] == null) {
                table[tableIndex] = node;
                newNodePlaced = true;
            } else {
                Jnode<K, V> currHeadNode = table[tableIndex];
                while (currHeadNode != null) {
                    if (currHeadNode.key.equals(key)) {
                        currHeadNode.value = value;//agar duplicate key hui tho overwrite o/w append at last.
                        return;
                    } else {
                        if (currHeadNode.next != null) {
                            currHeadNode = currHeadNode.next;
                        } else {
                            currHeadNode.next = node;
                            newNodePlaced = true;
                            break;
                        }
                    }
                }
            }
        }

        // coz i increment the node ct which is shared so no other thread should write to it; and also if resizing happens i hold the lock on the whole jmap instance;

        if (newNodePlaced) { // if no new node place just overwritten then no need to block can pass directly;
            synchronized (this) {
                System.out.println(Thread.currentThread().getName() + " got lock table" );
                int oldValue = node_ct.getAndIncrement();
                int newValue = node_ct.get();
                boolean resizeRequired = (node_ct.get() >= size.get() * resize_threshold);
                if (resizeRequired) {
                    resizeJmap();
                }
            }
        }

    }


    @Override
    public String toString() {

        StringBuilder str = new StringBuilder();

        for (int j = 0; j < size.get(); j++) {
            if (table[j] != null) {
                Jnode<K, V> tempNode = table[j];
                String formattedString = String.format("[%d] ->", j);
                str.append(formattedString);
                while (tempNode != null) {
                    //append every node to str;
                    str.append(tempNode).append("->");
                    tempNode = tempNode.next;
                }
                str.append("\n");
            } else {
                str.append("[").append(j).append("]->").append("null").append("\n");
            }
        }


        return str.toString();
    }


}