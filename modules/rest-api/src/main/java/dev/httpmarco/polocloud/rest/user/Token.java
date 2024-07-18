package dev.httpmarco.polocloud.rest.user;

public record Token(String token, long created, String hashedLastIp, String hashedLastUserAgent) {

    public Token(String token, String lastIp, String lastUserAgent) {
        this(token, System.currentTimeMillis() - 1000 * 60 * 60, lastIp, lastUserAgent);
    }

    public String toString() {
        return this.token;
    }
}