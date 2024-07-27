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
    public @Nullable ClusterGroup deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return null;
    }

    @Override
    public @NotNull JsonElement serialize(@NotNull ClusterGroup group, Type typeOfSrc, @NotNull JsonSerializationContext context) {
        var jsonObject = new JsonObject();

        jsonObject.addProperty("name", group.name());

        if (group.node().length == 1) {
            jsonObject.addProperty("nodes", group.node()[1]);
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

        if (group instanceof FallbackClusterGroup) {
            jsonObject.addProperty("fallback", true);
        }

        return jsonObject;
    }
}
