import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class KVStoreThreadSafetyTest {

    public static void main(String[] args) {

    }
    @Test
    void testConcurrentPuts() throws InterruptedException {
        Jmap<String,byte[]> store = new Jmap<>();
        ExecutorService executor = Executors.newFixedThreadPool(10);

        int numTasks = 100;
        CountDownLatch latch = new CountDownLatch(numTasks);

        for (int i = 0; i < numTasks; i++) {
            final int id = i;
            executor.submit(() -> {
                store.put("key" + id, ("value" + id).getBytes());
                latch.countDown();
            });
        }

        latch.await(); // wait for all threads to finish
        executor.shutdown();

        for (int i = 0; i < numTasks; i++) {
            byte[] arr = store.get("key"+i);

            if(arr!=null){
                String str = new String(arr, StandardCharsets.UTF_8);
                assertEquals("value" + i, str);
            }
            else{
                System.out.println("null");
            }
        }

        System.out.println(store);
    }

    @Test
    void testConcurrentReadWriteSameKey() throws InterruptedException {
        Jmap<String,byte[]> store = new Jmap<>();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        store.put("foo", ("init").getBytes());

        Runnable writer = () -> {
            for (int i = 0; i < 1000; i++) {
                store.put("foo", ("val" + i).getBytes());
            }
        };

        Runnable reader = () -> {
            for (int i = 0; i < 1000; i++) {
                String v = new String(store.get("foo"));
            }
            assertEquals("init", new String(store.get("foo")));
        };

        executor.submit(writer);
        executor.submit(reader);

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println(store);
    }
}