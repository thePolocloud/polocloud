package dev.httpmarco.polocloud.node.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JsonUtils {

    public final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

}
