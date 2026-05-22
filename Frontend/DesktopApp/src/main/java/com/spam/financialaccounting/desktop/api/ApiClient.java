package com.spam.financialaccounting.desktop.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.spam.financialaccounting.desktop.model.FAGroup;
import com.spam.financialaccounting.desktop.model.FASubGroup;
import com.spam.financialaccounting.desktop.model.JournalDetail;
import com.spam.financialaccounting.desktop.model.JournalMaster;

public class ApiClient {
    private static final String BASE_URL = "http://localhost:8080/api/v1";
    private final HttpClient client;
    private final ObjectMapper mapper;

    public ApiClient() {
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
                .build();
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
    }

    public CompletableFuture<Boolean> pingAsync() {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(BASE_URL + "/fagroups"))
                .timeout(Duration.ofSeconds(3)).GET().build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenApply(response -> response.statusCode() == 200).exceptionally(ex -> false);
    }

    // FAGROUP API
    public List<FAGroup> getGroups() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/fagroups")).GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        handleErrorResponse(response);
        return mapper.readValue(response.body(), new TypeReference<List<FAGroup>>() {
        });
    }

    public FAGroup createGroup(FAGroup group) throws IOException, InterruptedException {
        String json = mapper.writeValueAsString(group);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/fagroups"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        handleErrorResponse(response);
        return mapper.readValue(response.body(), FAGroup.class);
    }

    public FAGroup updateGroup(String code, FAGroup group) throws IOException, InterruptedException {
        String json = mapper.writeValueAsString(group);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/fagroups/" + code))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        handleErrorResponse(response);
        return mapper.readValue(response.body(), FAGroup.class);
    }

    // --- FASUBGROUP API (Ledgers) ---
    public List<FASubGroup> getLedgerAccounts() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/ledger-accounts"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        handleErrorResponse(response);
        return mapper.readValue(response.body(), new TypeReference<List<FASubGroup>>() {
        });
    }

    public FASubGroup createLedgerAccount(FASubGroup subGroup) throws IOException, InterruptedException {
        String json = mapper.writeValueAsString(subGroup);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/ledger-accounts"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        handleErrorResponse(response);
        return mapper.readValue(response.body(), FASubGroup.class);
    }

    public FASubGroup updateLedgerAccount(String code, FASubGroup subGroup) throws IOException, InterruptedException {
        String json = mapper.writeValueAsString(subGroup);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/ledger-accounts/" + code))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        handleErrorResponse(response);
        return mapper.readValue(response.body(), FASubGroup.class);
    }

    public void deleteLedgerAccount(String code) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/ledger-accounts/" + code))
                .DELETE()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        handleErrorResponse(response);
    }

    // --- JOURNAL MASTER ---
    public List<JournalMaster> getJournalMasters() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/journal-masters"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        handleErrorResponse(response);
        return mapper.readValue(response.body(), new TypeReference<List<JournalMaster>>() {
        });
    }

    public JournalMaster createJournalMaster(JournalMaster master) throws IOException, InterruptedException {
        String json = mapper.writeValueAsString(master);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/journal-masters"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        handleErrorResponse(response);
        return mapper.readValue(response.body(), JournalMaster.class);
    }

    public void deleteJournalMaster(String jId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/journal-masters/" + jId))
                .DELETE()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        handleErrorResponse(response);
    }

    // Journal detail
    public List<JournalDetail> getJournalDetailsByJournalId(String jId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/journal-details/journal/" + jId))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        handleErrorResponse(response);
        return mapper.readValue(response.body(), new TypeReference<List<JournalDetail>>() {
        });
    }

    public JournalDetail createJournalDetail(JournalDetail detail) throws IOException, InterruptedException {
        String json = mapper.writeValueAsString(detail);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/journal-details"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        handleErrorResponse(response);
        return mapper.readValue(response.body(), JournalDetail.class);
    }

    private void handleErrorResponse(HttpResponse<String> response) throws IOException {
        if (response.statusCode() >= 400) {
            String errorMsg = "HTTP Status Code " + response.statusCode();
            try {
                // parse standard Spring boot error format
                var map = mapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {
                });
                if (map.containsKey("message")) {
                    errorMsg = map.get("message").toString();
                } else if (map.containsKey("error")) {
                    errorMsg = map.get("error").toString();
                }
            } catch (Exception ignored) {
                throw new IOException(errorMsg);
            }
        }
    }
}