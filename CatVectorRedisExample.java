import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.util.HashMap;
import java.util.Map;
/*
Data Model: Each cat is stored as a Redis Hash with a name and a vector field.
The vector represents features (e.g., fur length, whisker length, etc.) as a float array.

Vector Storage: Vectors are stored as comma-separated strings since Redis doesn't natively
support float arrays.

Sample Data: Three cats are added with 4D vectors, and a query vector is used to find
similar cats.

Each cat has a name and a 4-dimensional vector representing some features
(e.g., fur length, whisker length, eye brightness, purr intensity—though these are
arbitrary for this example).

The code uses Euclidean distance to measure similarity between the query vector
and each cat’s vector.

 */

public class CatVectorRedisExample {
    private static final String REDIS_HOST = "localhost";
    private static final int REDIS_PORT = 6379;

    // Simulated vector dimension (e.g., 4D vector for cat features)
    private static final int VECTOR_DIMENSION = 4;

    public static void main(String[] args) {
        try (JedisPool jedisPool = new JedisPool(REDIS_HOST, REDIS_PORT)) {
            Jedis jedis = jedisPool.getResource();

            // Clear previous data (optional)
            jedis.flushDB();

            // Add sample cat data with vectors
            addCatData(jedis, "cat:1", "Whiskers", new float[]{0.1f, 0.8f, 0.3f, 0.6f});
            addCatData(jedis, "cat:2", "Mittens", new float[]{0.2f, 0.7f, 0.4f, 0.5f});
            addCatData(jedis, "cat:3", "Shadow", new float[]{0.9f, 0.2f, 0.1f, 0.3f});
            addCatData(jedis, "cat:4", "Alpha", new float[]{0.5f, 0.2f, 0.3f, 0.3f});
            addCatData(jedis, "cat:5", "Yoda", new float[]{0.8f, 0.1f, 0.1f, 0.4f});

            // Query vector for similarity search
            float[] queryVector = new float[]{0.15f, 0.75f, 0.35f, 0.55f};
            findSimilarCats(jedis, queryVector);
        }
    }

    // Add cat data with vector to Redis
    private static void addCatData(Jedis jedis, String key, String name, float[] vector) {
        Map<String, String> catData = new HashMap<>();
        catData.put("name", name);
        // Convert vector to string for storage
        String vectorStr = vectorToString(vector);
        catData.put("vector", vectorStr);

        // Store as a Redis Hash
        jedis.hset(key, catData);
        System.out.println("Added cat: " + name + " with vector: " + vectorStr);
    }

    // Convert float vector to string
    private static String vectorToString(float[] vector) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) sb.append(",");
        }
        return sb.toString();
    }

    // Convert string back to float vector
    private static float[] stringToVector(String vectorStr) {
        String[] parts = vectorStr.split(",");
        float[] vector = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vector[i] = Float.parseFloat(parts[i]);
        }
        return vector;
    }

    // Calculate Euclidean distance between two vectors
    private static double calculateDistance(float[] v1, float[] v2) {
        double sum = 0.0;
        for (int i = 0; i < v1.length; i++) {
            sum += Math.pow(v1[i] - v2[i], 2);
        }
        return Math.sqrt(sum);
    }

    // Find cats similar to the query vector
    private static void findSimilarCats(Jedis jedis, float[] queryVector) {
        System.out.println("\nSearching for similar cats...");

        ScanParams scanParams = new ScanParams().match("cat:*");
        String cursor = "0";

        do {
            ScanResult<String> scanResult = jedis.scan(cursor, scanParams);
            cursor = scanResult.getCursor();

            for (String key : scanResult.getResult()) {
                Map<String, String> catData = jedis.hgetAll(key);
                String name = catData.get("name");
                float[] vector = stringToVector(catData.get("vector"));

                double distance = calculateDistance(queryVector, vector);
                System.out.printf("Cat: %s, Distance: %.4f%n", name, distance);
            }
        } while (!"0".equals(cursor));
    }
}