import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KVStore {

    private static Jmap<String, byte[]> kvStore = new Jmap<>();
    private static PrintWriter logWriter;
    private static BufferedReader logReader;

    //right now every put, del goes to the log but log can bloat so need to compact it by frequently checking if dup enries so invalid entries are there or not.

    public static void main(String[] args) {
        //now before doing this we gotta build the kvStore from the log file;
        //and store all the updates to the log file;

        //in the log file writing as base64 encoding but in memory they are raw bytes only;
        fillStore();
        interactiveMode();


    }

    public static  void multithreadingTest(){
        int threadCount = 90;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
            int threadId = i;
            executor.submit(() -> {
                try {
                    String key = "key" + threadId;
                    String value = "value" + threadId;
                    kvStore.put(key, value.getBytes());

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
        System.out.println("All threads completed. size: " + Jmap.node_ct.get() + " <- noddees");
        System.out.println(kvStore);
    }



    public static void interactiveMode() {

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("> ");
            String command = scanner.nextLine().trim();
            switch (command.toUpperCase()) {
                case "PUT" -> {
                    System.out.print("---enter key: ");
                    String key = scanner.nextLine().trim();
                    System.out.print("------enter value: ");
                    String value = scanner.nextLine().trim();
                    logWriter.printf("PUT: %s %s%n", Base64.getEncoder().encodeToString(key.getBytes()), Base64.getEncoder().encodeToString(value.getBytes()));
                    //see here the value for the key can be complex so rather than jsut getting the 3rd arg get everything after the third arg and make it
                    logWriter.flush();
                    kvStore.put(key, value.getBytes());
                    System.out.println("OK");
                }
                case "GET" -> {
                    System.out.print("---enter key: ");
                    String key = scanner.nextLine().trim();
                    if (key.isEmpty()) {
                        System.out.println("enter key");
                        break;
                    }
                    byte[] bytes = kvStore.get(key);
                    if (bytes == null) {
                        System.out.println("key not found");
                        break;
                    }
                    String value = new String(bytes, StandardCharsets.UTF_8);
                    System.out.println(value);
                }
                case "DELETE" -> {
                    System.out.println("---enter key: ");
                    String key = scanner.nextLine();
                    logWriter.printf("DELETE: %s%n", key);
                    logWriter.flush();
                    kvStore.remove(key);
                    System.out.println("OK");
                }
                case "SHOW" -> System.out.println(kvStore);
                case "EXIT" -> {
                    return;
                }
                default -> System.out.println("Invalid command");
            }
        }

    }


    public static void fillStore() {
        try {
            logWriter = new PrintWriter(new FileWriter("src/main/logs/logs.txt", true));
            logReader = new BufferedReader(new FileReader("src/main/logs/logs.txt"));
            //now try to build the store from the logs;//the value in the logs are in base64 encoding so decode first and then store ;
            //1. read the file line by line;
            //2. parse the instructions decode them to simple strings -> perform the operation on the store;
            String log_instruction;
            while (true) {
                log_instruction = logReader.readLine();
                System.out.println(log_instruction);
                if (log_instruction == null) {
                    break;
                }
                //it'll either be a put(2args) or delete(1 arg)
                String[] instruction_args = log_instruction.split(" ");
                String instruction = instruction_args[0];
                switch (instruction.trim()) {
                    case "PUT:" -> {
                        //two args;
                        String key = instruction_args[1];//base64 encoded
                        byte[] byte_key = Base64.getDecoder().decode(key);
                        String value = instruction_args[2];
                        byte[] byte_value = Base64.getDecoder().decode(value);

                        kvStore.put(new String(byte_key), byte_value);
                    }
                    case "DELETE:" -> {
                        String key = instruction_args[1];//base64 encoded
                        byte[] byte_key = Base64.getDecoder().decode(key);
                        kvStore.remove(new String(byte_key));
                    }
                }
            }
        } catch (IOException e) {
            System.exit(1);
        }
    }
}
