package dev.httpmarco.polocloud.instance.config;

import dev.httpmarco.polocloud.api.config.ConfigProvider;
import lombok.SneakyThrows;

public final class ConfigProviderImpl implements ConfigProvider {
    @SneakyThrows
    public ConfigProviderImpl() {
        //TODO
    }

    @SneakyThrows
    @Override
    public <T> void createConfig(String fileName, T defaultValue) {
        throw new UnsupportedOperationException();
    }

    @SneakyThrows
    @Override
    public <T> T readConfig(String fileName, Class<T> tClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T readConfigOrCreate(String fileName, Class<T> tClass, T defaultValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean configExists(String fileName) {
        throw new UnsupportedOperationException();
    }
}
