package dev.httpmarco.polocloud.rest.endpoints;

import com.google.gson.JsonObject;
import dev.httpmarco.polocloud.rest.RestAPI;
import dev.httpmarco.polocloud.rest.user.User;
import io.javalin.http.Context;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@AllArgsConstructor
@Accessors(fluent = true)
public abstract class Endpoint implements EndpointMethods {

    private final String path;
    private final RestAPI restAPI;

    public JsonObject parseJSON(String body) {
        try {
            return RestAPI.GSON.fromJson(body, JsonObject.class);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean tokenProvided(Context context) {
        var response = new JsonObject();
        var auth = context.header("Authorization");
        if (auth == null) {
            response.addProperty("message", "No token provided");
            context.status(401).result(response.toString());
            return false;
        }

        var authArray = auth.split(" ");
        if (authArray.length != 2 || !authArray[0].equals("Bearer")) {
            response.addProperty("message", "Token is malformed");
            context.status(400).result(response.toString());
            return false;
        }

        return true;
    }

    // first tokenProvided needs to be called
    public String getToken(Context context) {
        return context.header("Authorization").split(" ")[1];
    }

    public User handelAuth(Context context, String token) {
        var response = new JsonObject();
        var user = this.restAPI.userManager().findUserByToken(token, context.ip());
        if (user == null) {
            response.addProperty("message", "Invalid credentials");
            context.status(401).result(response.toString());
            return null;
        }

        return user;
    }

    public boolean hasPermission(User user, String permission, Context context) {
        var response = new JsonObject();
        if (!user.hasPermission(permission)) {
            response.addProperty("message", "No permission");
            context.status(403).result(response.toString());
            return false;
        }

        return true;
    }
}
