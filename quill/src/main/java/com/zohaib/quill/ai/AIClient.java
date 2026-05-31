package com.zohaib.quill.ai;

import com.google.gson.*;
import com.zohaib.quill.config.ConfigManager;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * AIClient — Handles all HTTP communication with the Groq REST API endpoint.
 * Uses OkHttp for networking and Gson for JSON serialisation/deserialisation.
 *
 * <p>This class is intentionally stateless (other than the shared OkHttpClient
 * instance, which is thread-safe). Callers are responsible for running AI
 * requests on a background thread (e.g., via {@code SwingWorker}) to avoid
 * blocking the Event Dispatch Thread.
 *
 * <p>Endpoint: {@code POST https://api.groq.com/openai/v1/chat/completions}
 *
 * @author Zohaib
 * @version 1.0
 */
public class AIClient {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    private static final String ENDPOINT =
        "https://api.groq.com/openai/v1/chat/completions";

    private static final String MODEL = "llama-3.3-70b-versatile";

    private static final String SYSTEM_PROMPT =
        "You are a helpful writing and coding assistant embedded in a text editor called Quill. "
        + "Be concise, clear, and professional in all responses.";

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static final int MAX_TOKENS       = 1024;
    private static final int CONNECT_TIMEOUT  = 15;  // seconds
    private static final int READ_TIMEOUT     = 60;  // seconds
    private static final int WRITE_TIMEOUT    = 15;  // seconds

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    private final ConfigManager configManager;
    private final OkHttpClient  httpClient;
    private final Gson          gson;

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------

    /**
     * Creates an AIClient backed by the given {@link ConfigManager} for API key retrieval.
     *
     * @param configManager the config manager supplying the Groq API key
     */
    public AIClient(ConfigManager configManager) {
        this.configManager = configManager;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT,       TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT,     TimeUnit.SECONDS)
            .build();
        this.gson = new GsonBuilder().create();
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Sends a "Fix &amp; Rephrase" request to Groq.
     * The response contains a rewritten, improved version of the input text.
     *
     * @param selectedText the text selected by the user to be fixed/rephrased
     * @return the AI-improved text
     * @throws AIException if the request fails or the API key is missing/invalid
     */
    public String fixAndRephrase(String selectedText) throws AIException {
        String prompt =
            "Fix any grammar, spelling, and style issues in the following text, "
            + "then rephrase it to sound more professional and clear. "
            + "Return ONLY the improved text with no explanation or preamble:\n\n"
            + selectedText;
        return chat(prompt);
    }

    /**
     * Sends a "Summarize" request to Groq.
     *
     * @param selectedText the text to summarise
     * @return a concise summary of the input text
     * @throws AIException if the request fails
     */
    public String summarize(String selectedText) throws AIException {
        String prompt =
            "Provide a concise, well-structured summary of the following text. "
            + "Use bullet points where appropriate:\n\n"
            + selectedText;
        return chat(prompt);
    }

    /**
     * Sends an "Explain Code" request to Groq.
     *
     * @param selectedCode the code snippet to explain
     * @return a plain-English explanation of the code
     * @throws AIException if the request fails
     */
    public String explainCode(String selectedCode) throws AIException {
        String prompt =
            "Explain the following code in clear, plain English suitable for a developer "
            + "who is unfamiliar with this particular code. Describe what it does, "
            + "how it works, and any important patterns or techniques used:\n\n"
            + selectedCode;
        return chat(prompt);
    }

    // -----------------------------------------------------------------------
    // Internal HTTP logic
    // -----------------------------------------------------------------------

    /**
     * Builds and executes a chat-completion request with a single user message.
     *
     * @param userPrompt the user message content
     * @return the AI response text
     * @throws AIException on any error (network, auth, parse, etc.)
     */
    private String chat(String userPrompt) throws AIException {
        // Guard: API key must be configured
        if (!configManager.isApiKeyConfigured()) {
            throw new AIException(
                "No Groq API key configured.\n\n"
                + "Please open config.properties and set:\n"
                + "  GROQ_API_KEY=your_actual_key\n\n"
                + "Get a free key at: https://console.groq.com",
                AIException.Cause.MISSING_API_KEY
            );
        }

        // Build JSON request body
        JsonObject body = buildRequestBody(userPrompt);
        RequestBody requestBody = RequestBody.create(gson.toJson(body), JSON);

        Request request = new Request.Builder()
            .url(ENDPOINT)
            .addHeader("Authorization", "Bearer " + configManager.getApiKey())
            .addHeader("Content-Type",  "application/json")
            .post(requestBody)
            .build();

        // Execute and parse
        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";

            if (response.code() == 401) {
                throw new AIException(
                    "Invalid API key (HTTP 401).\n\n"
                    + "Please update GROQ_API_KEY in config.properties.",
                    AIException.Cause.INVALID_API_KEY
                );
            }

            if (!response.isSuccessful()) {
                throw new AIException(
                    "Groq API returned HTTP " + response.code() + ".\n"
                    + "Please check your internet connection and try again.\n\n"
                    + "Details: " + extractErrorMessage(responseBody),
                    AIException.Cause.HTTP_ERROR
                );
            }

            return extractContent(responseBody);

        } catch (IOException e) {
            throw new AIException(
                "Network error while contacting Groq API:\n" + e.getMessage()
                + "\n\nPlease check your internet connection.",
                AIException.Cause.NETWORK_ERROR
            );
        }
    }

    /**
     * Constructs the OpenAI-compatible JSON request body.
     *
     * @param userPrompt user message text
     * @return populated {@link JsonObject}
     */
    private JsonObject buildRequestBody(String userPrompt) {
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role",    "system");
        systemMsg.addProperty("content", SYSTEM_PROMPT);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role",    "user");
        userMsg.addProperty("content", userPrompt);

        JsonArray messages = new JsonArray();
        messages.add(systemMsg);
        messages.add(userMsg);

        JsonObject body = new JsonObject();
        body.addProperty("model",      MODEL);
        body.add("messages",           messages);
        body.addProperty("max_tokens", MAX_TOKENS);

        return body;
    }

    /**
     * Extracts the AI response text from the raw JSON response string.
     *
     * @param responseBody raw JSON from Groq
     * @return the content string from {@code choices[0].message.content}
     * @throws AIException if the response cannot be parsed
     */
    private String extractContent(String responseBody) throws AIException {
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray choices = json.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw new AIException("Empty choices array in Groq response.",
                    AIException.Cause.PARSE_ERROR);
            }
            JsonObject firstChoice = choices.get(0).getAsJsonObject();
            JsonObject message = firstChoice.getAsJsonObject("message");
            return message.get("content").getAsString().trim();
        } catch (JsonParseException | IllegalStateException | NullPointerException e) {
            throw new AIException(
                "Could not parse Groq API response: " + e.getMessage(),
                AIException.Cause.PARSE_ERROR
            );
        }
    }

    /**
     * Attempts to extract a human-readable error message from an error response body.
     *
     * @param body raw response body
     * @return error message string, or the body itself if parsing fails
     */
    private String extractErrorMessage(String body) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (json.has("error")) {
                JsonObject err = json.getAsJsonObject("error");
                if (err.has("message")) {
                    return err.get("message").getAsString();
                }
            }
        } catch (Exception ignored) {
            // Fall through
        }
        return body.length() > 300 ? body.substring(0, 300) + "..." : body;
    }

    // -----------------------------------------------------------------------
    // Nested exception type
    // -----------------------------------------------------------------------

    /**
     * Typed exception thrown by {@link AIClient} for all AI-related failures.
     * The {@link Cause} enum lets callers differentiate error categories.
     */
    public static class AIException extends Exception {

        /** Categories of AI errors. */
        public enum Cause {
            MISSING_API_KEY,
            INVALID_API_KEY,
            HTTP_ERROR,
            NETWORK_ERROR,
            PARSE_ERROR
        }

        private final Cause cause;

        /**
         * Constructs an AIException with a message and categorised cause.
         *
         * @param message human-readable description
         * @param cause   error category
         */
        public AIException(String message, Cause cause) {
            super(message);
            this.cause = cause;
        }

        /**
         * Returns the error category.
         *
         * @return cause enum value
         */
        public Cause getAICause() {
            return cause;
        }
    }
}
