package org.example;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class DateChecker {
    private static final String BASE_URL = "https://inpol.mazowieckie.pl/api/reservations/queue/";
    private static final Map<String, String> QUEUES = Map.of(
            "pl. Bankowy 3/5 00-950 Warszaw", "f0992a78-802d-40e7-9bd0-c0d8d46a71fd",
            "Al. Jerozolimskie 28, 00-024 Warszawa", "c93674d6-fb24-4a85-9dac-61897dc8f060",
            "ul. Marszałkowska 3/5, 00-624 Warszawa", "3ab99932-8e53-4dff-9abf-45b8c6286a99"
    );
    private static final List<String> DATES = Arrays.asList("2023-05-16", "2023-05-17", "2023-05-18", "2023-05-19", "2023-05-22");
    private static final String EMPTY_REQUEST_BODY = "{}";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static String BEARER_TOKEN =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJiMWJkNjQyNS1kOWRiLTRkMDAtYmYyNC1iNTVkNzViNTJiYzYiLCJ1bmlxdWVfbmFtZSI6ImIxYmQ2NDI1LWQ5ZGItNGQwMC1iZjI0LWI1NWQ3NWI1MmJjNiIsImp0aSI6IjAxZDE4M2ExLTlmYmYtNDMzMy05NmM4LTg1ZGRiNmUzNWVjOCIsImlhdCI6MTY4NDE0MTAyNywiZGlzcGxheU5hbWUiOiJmZXpoZW5rb0BnbWFpbC5jb20iLCJuYmYiOjE2ODQxNDEwMjcsImV4cCI6MTY4NDE0MTkyNywiaXNzIjoiaW5wb2wtZGlyZWN0IiwiYXVkIjoiaW5wb2wtZGlyZWN0In0.ybtD8B8cuZtDued4mdIcwv5WQqhVIGBsGIgh5oZEWWw";

    public static void main(String[] args) {
        while (true) {
            final AtomicBoolean foundSlots = new AtomicBoolean(false);
            ExecutorService executor = Executors.newFixedThreadPool(DATES.size());

            for (final String date : DATES) {
                for (final Map.Entry<String, String> queue : QUEUES.entrySet()) {
                    final String url = BASE_URL + queue.getValue() + "/" + date + "/slots";

                    executor.submit(
                            () -> {
                                HttpURLConnection connection = null;
                                try {
                                    connection = (HttpURLConnection) new URL(url).openConnection();

                                    // Try using POST method
                                    connection.setRequestMethod("POST");
                                    connection.setDoOutput(true);

                                    byte[] postData = EMPTY_REQUEST_BODY.getBytes();
                                    int contentLength = postData.length;
                                    connection.setRequestProperty("Content-Type", "application/json");
                                    connection.setRequestProperty("Content-Length", Integer.toString(contentLength));
                                    connection.setRequestProperty(AUTHORIZATION_HEADER, BEARER_PREFIX + BEARER_TOKEN);

                                    try (OutputStream outputStream = connection.getOutputStream()) {
                                        outputStream.write(postData);
                                    }

                                    if (connection.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST) {
                                        System.out.println("No slots for " + queue.getKey() + " on " + date);

                                    } else if (connection.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                                        System.out.println("Unauthorized access. Trying to reauthorize...");
                                        // Try to reauthorize
                                        BEARER_TOKEN = getNewBearerToken();

                                    } else {
                                        foundSlots.set(true);
                                        log.error("SLOT!");
                                        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                                        String response = in.readLine();

                                        System.out.println("Available slot for " + queue.getKey() + " on " + date.toUpperCase());
                                        System.out.println(response);
                                        in.close();
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } finally {
                                    if (connection != null) {
                                        connection.disconnect();
                                    }
                                }
                            }
                    );
                }
            }

            executor.shutdown();

            try {
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (!foundSlots.get()) {
                System.out.println("No slots found.".toUpperCase());
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static String getNewBearerToken() throws IOException {
        // Code to retrieve new bearer token goes here
        // Replace the following line with your own code to retrieve the new token
        return "your-new-bearer-token-here";
    }
}
