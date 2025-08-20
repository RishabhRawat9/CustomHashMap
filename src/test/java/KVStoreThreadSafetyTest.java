import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KVStoreThreadSafetyTest {

    public static void main(String[] args) {
        Jmap<String, String> jmap = new Jmap<>();
        ConcurrentHashMap<String, String> concurrentMap = new ConcurrentHashMap<>();
        int threadCount = 40;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            int threadId = i;
            executor.submit(() -> {
                try {
                    String key = "key" + (threadId % 5); // Simulate key collisions
                    String value = "value" + threadId;

                    // Perform PUT operation
                    jmap.put(key, value);
                    concurrentMap.put(key, value);

                    // Perform GET operation
                    String jmapValue = jmap.get(key);
                    String concurrentMapValue = concurrentMap.get(key);

                    if (!value.equals(jmapValue) || !value.equals(concurrentMapValue)) {
                        System.err.printf("Mismatch detected! Key: %s, Expected: %s, Jmap: %s, ConcurrentMap: %s%n",
                                key, value, jmapValue, concurrentMapValue);
                    }

                    // Perform REMOVE operation
                    jmap.remove(key);
                    concurrentMap.remove(key);
                } catch (Exception e) {
                    System.err.println("Error in thread " + threadId + ": " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await(); // Wait for all threads to finish
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        executor.shutdown();
        System.out.println("All threads completed.");
        System.out.println("Final Jmap state:");
        System.out.println(jmap);
        System.out.println("Final ConcurrentHashMap state:");
        System.out.println(concurrentMap);
    }
}