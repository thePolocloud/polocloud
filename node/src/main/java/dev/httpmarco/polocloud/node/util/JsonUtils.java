package dev.httpmarco.polocloud.node.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.httpmarco.polocloud.node.platforms.Platform;
import dev.httpmarco.polocloud.node.platforms.util.PlatformTypeAdapter;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JsonUtils {

    public final Gson GSON = new GsonBuilder().setPrettyPrinting()
            .registerTypeAdapter(Platform.class, PlatformTypeAdapter.INSTANCE)
            .create();

}
