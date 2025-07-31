public class Jmap<K,V> {
    private Jnode<K, V>[] table;
    private final int size=16; //16 buckets only.;


    @SuppressWarnings("unchecked")
    public Jmap() {
        this.table = (Jnode<K, V>[]) new Jnode[size];

    }

    private int hashFunction(K key){
        //so when a key is provided it returns a hash value;
        int hashcode = key.hashCode();
        int hashValue = hashcode % size;
        return hashValue;
    }

    public void put(K key, V value){
        //so i first run the hashFunction and then put it on that index in the arry;
        // i would also have to check if values are already at that index then i would need to chain them;
        int tableIndex = hashFunction(key);
        //now a node value is constructed;
        Jnode<K, V> node = new Jnode<>(tableIndex, key, value);
        //now check if bucket is empty or not
        if(table[tableIndex]==null){
            table[tableIndex] = node;
        }
        else{
            //i'll go to the last node and place the new node there, but if i do this then the putting of values would be O(N) in case of colisions so to prevent that i would need a end pointer to the chained linked list;
            //but i still have to ensure that no key dups are there so i anyways need to traverse the whole thing in case of collisions;

            Jnode<K,V> currHeadNode = table[tableIndex];
            while(currHeadNode!=null){
                if(currHeadNode.key.equals(key)){
                    currHeadNode.value = value;//agar duplicate key hui tho overwrite o/w append at last.
                    return;
                }
                else{
                    currHeadNode=currHeadNode.next;
                }
            }
        }
    }


    public Jnode<K,V> get(K key){
        //ab mujhe is key ka hashvalue chaiye
        int tableIndex = hashFunction(key);
        //now i can search for this key in my table;
        Jnode<K,V> value = table[tableIndex];// this right here is the head of the linked list if muliple nodes reside in the same bucket.
        //now go through the the collided nodes and check their hashcode

        while(value!=null){
            //check each nodes key.equals();
            if(value.key.equals(key)){
                return value;
            }
            else{
                value=value.next;
            }
        }
        return null;
    }


    @Override
    public String toString(){

        StringBuilder str = new StringBuilder();


        for(int j=0;j<size;j++){

            if(table[j]!=null){
                Jnode<K,V> tempNode = table[j];
                String formattedString = String.format("[%d] ->", j);
                str.append(formattedString);
                while(tempNode!=null){
                    //append every node to str;
                    str.append(tempNode).append("->");
                    tempNode=tempNode.next;
                }
                str.append("\n");
            }
            else{
                str.append("[").append(j).append("]->").append("null").append("\n");
            }

        }


        return str.toString();
    }






}