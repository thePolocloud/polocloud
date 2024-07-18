package dev.httpmarco.polocloud.rest.user;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record User(String username, String hashedPassword, UUID uuid, List<Token> tokens, List<String> permissions, long created) {

    public User(String username, String tokens, UUID uuid) {
        this(username, tokens, uuid, new ArrayList<>(), new ArrayList<>(), System.currentTimeMillis());
    }

    public boolean hasPermission(String permission) {
        if (this.permissions.contains("*")) {
            return true;
        }

        return this.permissions.contains(permission);
    }

    public JsonObject toJSONObject() {
        var jsonObject = new JsonObject();

        jsonObject.addProperty("name", this.username);
        jsonObject.addProperty("uuid", this.uuid.toString());
        jsonObject.addProperty("created", this.created);

        return jsonObject;
    }
}