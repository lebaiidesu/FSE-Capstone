package com.bank.common.dto;

public class LoginResponse {

    private String token;
    private String tokenType = "Bearer";
    private Long customerId;
    private String username;
    private String fullName;
    private String email;

    public LoginResponse() {}

    public LoginResponse(String token, Long customerId, String username, String fullName, String email) {
        this.token = token;
        this.customerId = customerId;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
