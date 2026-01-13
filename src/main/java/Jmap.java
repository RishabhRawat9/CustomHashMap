import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Jmap<K, V> {
    private Jnode<K, V>[] table;
    private final AtomicInteger size = new AtomicInteger(16);
    private float resize_threshold = 0.75f;// so when 75% of the capacity is filled i resize the thing right;
    public static volatile AtomicInteger node_ct = new AtomicInteger(0);

    private ReadWriteLock[] rwLocks;
    private ReadWriteLock globalRwLock = new ReentrantReadWriteLock();
    private Lock resizeLock = globalRwLock.writeLock();//only acquire this at the time of resizing;

    @SuppressWarnings("unchecked")
    public Jmap() {
        this.table = (Jnode<K, V>[]) new Jnode[size.get()];
        this.rwLocks = new ReentrantReadWriteLock[size.get()];
        for (int j = 0; j < size.get(); j++) {
            rwLocks[j] = new ReentrantReadWriteLock();
        }
    }

    @SuppressWarnings("unchecked")
    public Jmap(float threshold) {
        this.table = (Jnode<K, V>[]) new Jnode[size.get()];
        this.resize_threshold = threshold;
        this.rwLocks = new ReentrantReadWriteLock[size.get()];
        for (int j = 0; j < size.get(); j++) {
            rwLocks[j] = new ReentrantReadWriteLock();
        }
    }

    // now every time a new node is placed i check if resizing is required or not;

    private int hashFunction(K key, int mapCapacity) {
        // so when a key is provided it returns a hash value;
        int h = key.hashCode();

        return h % (mapCapacity);
    }

    public void put(K key, V value) {

        int tableIndex = hashFunction(key, size.get());
        boolean newNodePlaced = false;
        Jnode<K, V> node = new Jnode<>(tableIndex, key, value);
        synchronized (rwLocks[tableIndex]) { /// one thread at a time only looks at this bucket all other buckets
            /// are free to be modified by other threads;
            System.out.println(Thread.currentThread().getName() + " got lock " + tableIndex);
            if (table[tableIndex] == null) {
                table[tableIndex] = node;
                node_ct.getAndIncrement();
                newNodePlaced = true;
            } else {
                Jnode<K, V> currHeadNode = table[tableIndex];
                while (currHeadNode != null) {
                    if (currHeadNode.key.equals(key)) {
                        currHeadNode.value = value;// agar duplicate key hui tho overwrite o/w append at last.
                        return;
                    } else {
                        if (currHeadNode.next != null) {
                            currHeadNode = currHeadNode.next;
                        } else {
                            currHeadNode.next = node;
                            newNodePlaced = true;
                            node_ct.getAndIncrement();
                            break;
                        }
                    }
                }
            }
        }

        if (newNodePlaced) { // if no new node place just overwritten then no need to block can pass
            // directly;
            synchronized (this) {
                System.out.println(node_ct.get());
                System.out.println(Thread.currentThread().getName() + " got lock table");
                boolean resizeRequired = (node_ct.get() >= size.get() * resize_threshold);
                if (resizeRequired) {
                    resizeJmap();
                }
            }
        }

    }


    public void remove(K key) {
        int tableIndex = hashFunction(key, size.get());
        synchronized (rwLocks[tableIndex]) {
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

            if (prev == null) {// if it's the first node only;;
                table[tableIndex] = current.next;
            } else {
                prev.next = current.next; // deleted the curr one gc removes it;
            }
            node_ct.getAndDecrement();
        }

    }

    public V get(K key) {
        //so this is a read i just need to acquire the global read lock and the bucket level read lock;
        globalRwLock.readLock().lock(); //blocks incase a resize is happening;
        int tableIndex = hashFunction(key, size.get());
        ReadWriteLock bucketLock = rwLocks[tableIndex];
        bucketLock.readLock().lock();
        //prevents reads and writes on same bucket at the saem time , and it allows multiple reads to happen together, previously reads would've blocked.
        try{
           Jnode<K, V> value = table[tableIndex];
           while (value != null) {
               if (value.key.equals(key)) {
                   return value.value;
               } else {
                   value = value.next;
               }
           }
           return null;
       }finally { ///always executes;
           bucketLock.readLock().unlock();
           globalRwLock.readLock().unlock();
       }
    }

    // now for these resize operations i gotta block all other threads and just
    // resize other wise data is corrupted while resizing a put is done or something
    // else then the map is fucked;
    // gotta lock the entire table so that no new operations can't be done until
    // resizing is done;

    public void resize_put(K key, V value, Jnode<K, V>[] newTable) {
        int tableIndex = hashFunction(key, newTable.length);
        Jnode<K, V> node = new Jnode<>(tableIndex, key, value);
        if (newTable[tableIndex] == null) {
            newTable[tableIndex] = node;
            node_ct.getAndIncrement();
        } else {
            Jnode<K, V> currHeadNode = newTable[tableIndex];
            while (currHeadNode != null) {
                if (currHeadNode.next != null) {
                    currHeadNode = currHeadNode.next;
                } else {
                    currHeadNode.next = node;
                    node_ct.getAndIncrement();
                    return;
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void resizeJmap() {
        // so a completely new array with twice the size is required now
        int newSize = size.get() * 2;
        Jnode<K, V>[] newTable = (Jnode<K, V>[]) new Jnode[newSize];
        // now all entries need to be traversed and rehashed and put into the new
        // bucket;
        for (int j = 0; j < size.get(); j++) {
            // now for each bucket traverse all the nodes; and place them in new arr;
            Jnode<K, V> currNode = table[j];
            synchronized (rwLocks[j]) {
                while (currNode != null) {
                    resize_put(currNode.key, currNode.value, newTable);
                    currNode = currNode.next;
                }
            }
        }
        // after resziing is done you also have to allocate more bucket locks for the
        // new arr right/
        this.table = newTable;
        this.size.set(newSize);
        this.rwLocks = new ReentrantReadWriteLock[size.get()];
        for (int j = 0; j < size.get(); j++) {
            rwLocks[j] = new ReentrantReadWriteLock();// stupid had to allocate more locks aswell ;
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
                    // append every node to str;
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