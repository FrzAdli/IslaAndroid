package com.example.isla_beta.models;

public class PromptMessage {
    private final String role;
    private final String content;

    public PromptMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }
}
