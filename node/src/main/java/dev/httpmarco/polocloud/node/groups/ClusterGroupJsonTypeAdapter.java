package dev.httpmarco.polocloud.node.groups;

import com.google.gson.*;
import dev.httpmarco.polocloud.api.groups.ClusterGroup;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

public final class ClusterGroupJsonTypeAdapter implements JsonSerializer<ClusterGroup>, JsonDeserializer<ClusterGroup> {

    @Contract(pure = true)
    @Override
    public @NotNull ClusterGroup deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        var jsonObject = json.getAsJsonObject();

        var name = jsonObject.get("name").getAsString();

        if (!jsonObject.has("minMemory")) {
            throw new JsonParseException("Cannot load group " + name + ", because min memory is missing!");
        }
        var minMemory = jsonObject.get("minMemory").getAsInt();

        if (!jsonObject.has("maxMemory")) {
            throw new JsonParseException("Cannot load group " + name + ", because min maxMemory is missing!");
        }
        var maxMemory = jsonObject.get("maxMemory").getAsInt();

        if (!jsonObject.has("staticService")) {
            throw new JsonParseException("Cannot load group " + name + ", because min staticService is missing!");
        }
        var staticService = jsonObject.get("staticService").getAsBoolean();

        if (!jsonObject.has("minOnlineServerInstances")) {
            throw new JsonParseException("Cannot load group " + name + ", because min minOnlineServerInstances is missing!");
        }
        var minOnlineServerInstances = jsonObject.get("minOnlineServerInstances").getAsInt();

        if (!jsonObject.has("maxOnlineServerInstances")) {
            throw new JsonParseException("Cannot load group " + name + ", because min maxOnlineServerInstances is missing!");
        }
        var maxOnlineServerInstances = jsonObject.get("maxOnlineServerInstances").getAsInt();

        return new ClusterGroupImpl(name, null, null, minMemory, maxMemory, staticService, minOnlineServerInstances, maxOnlineServerInstances);
    }

    @Override
    public @NotNull JsonElement serialize(@NotNull ClusterGroup group, Type typeOfSrc, @NotNull JsonSerializationContext context) {
        var jsonObject = new JsonObject();

        jsonObject.addProperty("name", group.name());

        if (group.node().length == 1) {
            jsonObject.addProperty("nodes", group.node()[0]);
        } else {
            var nodeArray = new JsonArray();

            for (String node : group.node()) {
                nodeArray.add(node);
            }

            jsonObject.add("nodes", nodeArray);
        }

        jsonObject.add("platform", context.serialize(group.platform()));
        jsonObject.addProperty("minMemory", group.minMemory());
        jsonObject.addProperty("maxMemory", group.maxMemory());

        jsonObject.addProperty("minOnlineServerInstances", group.minOnlineServerInstances());
        jsonObject.addProperty("maxOnlineServerInstances", group.maxOnlineServerInstances());

        jsonObject.addProperty("staticService", group.staticService());

        if (group instanceof FallbackClusterGroup) {
            jsonObject.addProperty("fallback", true);
        }

        return jsonObject;
    }
}
