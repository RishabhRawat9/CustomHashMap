

public class Jmap<K, V> {
    private Jnode<K, V>[] table;
    private int size = 16; //16 buckets only.;
    private float resize_threshold = 0.75f;//so when 75% of the capacity is filled i resize the thing right;

    private static int node_ct;

    @SuppressWarnings("unchecked")
    public Jmap() {
        this.table = (Jnode<K, V>[]) new Jnode[size];
    }

    @SuppressWarnings("unchecked")
    public Jmap(float threshold) {
        this.table = (Jnode<K, V>[]) new Jnode[size];
        this.resize_threshold = threshold;
    }
    //now every time a new node is placed i check if resizing is required or not;

    private int hashFunction(K key, int mapCapacity) {
        //so when a key is provided it returns a hash value;
        int hashcode = key.hashCode();
        int hashValue = hashcode % mapCapacity;
        return hashValue;
    }

    public void remove(K key) {
        int tableIndex = hashFunction(key, size);
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
        node_ct--;
    }

    public void put(K key, V value) {
        int tableIndex = hashFunction(key, size);
        boolean newNodePlaced = false;
        Jnode<K, V> node = new Jnode<>(tableIndex, key, value);
        if (table[tableIndex] == null) {
            table[tableIndex] = node;
            newNodePlaced = true;
        } else {
            //i'll go to the last node and place the new node there, but if i do this then the putting of values would be O(N) in case of colisions so to prevent that i would need a end pointer to the chained linked list;
            //but i still have to ensure that no key dups are there so i anyways need to traverse the whole thing in case of collisions;
            //i am not placing the new node;

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
                    }
                }
            }
        }
        if (newNodePlaced) {
            //check the no. of items and perform the resizing if required
            node_ct += 1;

            if (node_ct>=size * resize_threshold) {
                System.out.println("resize processing");
                resizeJmap();
            }
        }
    }

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
        int newSize = size * 2;
        Jnode<K, V>[] newTable = (Jnode<K, V>[]) new Jnode[newSize];
        //now all entries need to be traversed and rehashed and put into the new bucket;
        for (int j = 0; j < size; j++) {
            //now for each bucket traverse all the nodes; and place them in new arr;
            Jnode<K, V> currNode = table[j];
            while (currNode != null) {
                resize_put(currNode.key, currNode.value, newTable);
                currNode = currNode.next;
            }
        }
        this.table = newTable;
        this.size= newSize;
    }


    public V get(K key) {
        //ab mujhe is key ka hashvalue chaiye
        int tableIndex = hashFunction(key, size);
        //now i can search for this key in my table;
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


    @Override
    public String toString() {

        StringBuilder str = new StringBuilder();


        for (int j = 0; j < size; j++) {

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