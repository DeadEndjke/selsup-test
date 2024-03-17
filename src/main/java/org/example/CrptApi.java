package org.example;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import okhttp3.*;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private final OkHttpClient client;
    private final Semaphore semaphore;
    private final ObjectMapper objectMapper;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        client = new OkHttpClient();
        semaphore = new Semaphore(requestLimit);
        objectMapper = new ObjectMapper();

        try (ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1)) {
            scheduler.scheduleAtFixedRate(() -> semaphore.release(requestLimit - semaphore.availablePermits()), 0, 1, timeUnit);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createDocument(Document document, String signature) {
        try {
            semaphore.acquire();
            String jsonBody = objectMapper.writeValueAsString(document);

            RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url("https://ismp.crpt.ru/api/v3/lk/documents/create")
                    .post(body)
                    .header("Signature", signature)
                    .build();

            Call call = client.newCall(request);
            try (Response response = call.execute()) {
                assert response.body() != null;
                System.out.println(response.body().string());
                System.out.println(response.body().string());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            semaphore.release();
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    static class Document {
        @JsonProperty("description")
        private Description description;

        @JsonProperty("doc_id")
        private String docId;

        @JsonProperty("doc_status")
        private String docStatus;

        @JsonProperty("doc_type")
        private String docType;

        @JsonProperty("importRequest")
        private boolean importRequest;

        @JsonProperty("owner_inn")
        private String ownerInn;

        @JsonProperty("participant_inn")
        private String participantInn;

        @JsonProperty("producer_inn")
        private String producerInn;

        @Getter
        @Setter
        @AllArgsConstructor
        public static class Description {
            @JsonProperty("participantInn")
            private String participantInn;


        }
    }


    public static void main(String[] args) {
        CrptApi api = new CrptApi(TimeUnit.SECONDS, 10);


        Document document = new Document();
        String signature = "example_signature";
        api.createDocument(document, signature);
    }
}
