package com.open.ai.chatbot.open.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

@Component
public class ChatbotApp {
    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final String URL_API = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    @PostConstruct
    public void init() throws IOException, InterruptedException {
        List<Content> contents = new ArrayList<>();
        Map<String, List<Content>> body = new HashMap<>();

        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to Chatbot Gemini AI. type q or Q to exit");
        while (true) {
            System.out.print("Please ask me anything: ");
            String input = scanner.nextLine();
            if (input.trim().equals("q") || input.trim().equals("Q")) {
                break;
            }

            body = getBody(input, "user", contents);
            ObjectMapper mapper = new ObjectMapper();
            String jsonBody = mapper.writeValueAsString(body);

            var request = HttpRequest.newBuilder()
                    .uri(URI.create(URL_API))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", geminiApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = mapper.readTree(response.body());

                // Ambil path: candidates[0].content.parts[0].text
                String answer = root.path("candidates")
                        .path(0)
                        .path("content")
                        .path("parts")
                        .path(0)
                        .path("text")
                        .asText();

                System.out.println("-> " + answer);
                getBody(answer, "model", contents);
            }
        }
    }

    private Map<String, List<Content>> getBody(String input, String role, List<Content> oldContent) {
        Content content = new Content();
        content.setRole(role);

        List<Map<String, String>> parts = new ArrayList<>();
        Map<String, String> part = new HashMap<>();
        part.put("text", input);
        parts.add(part);

        content.setParts(parts);

        oldContent.add(content);

        Map<String, List<Content>> result = new HashMap<>();
        result.put("contents", oldContent);

        return result;
    }
}

class Content {
    private String role;
    private List<Map<String, String>> parts;


    public List<Map<String, String>> getParts() {
        return parts;
    }

    public void setParts(List<Map<String, String>> parts) {
        this.parts = parts;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
