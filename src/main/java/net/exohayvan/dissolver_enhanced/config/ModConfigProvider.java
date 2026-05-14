package net.exohayvan.dissolver_enhanced.config;


import net.exohayvan.dissolver_enhanced.common.config.ConfigEntry;

import java.util.ArrayList;
import java.util.List;

public class ModConfigProvider implements SimpleConfig.DefaultConfig {

    private String configContents = "";

    public List<ConfigEntry<?>> getConfigsList() {
        return configsList;
    }

    private final List<ConfigEntry<?>> configsList = new ArrayList<>();

    public void addComment(String comment) {
        configContents += "# " + comment + "\n";
    }

    public void addKeyValuePair(ConfigEntry<?> keyValuePair, String comment) {
        configsList.add(keyValuePair);
        configContents += keyValuePair.getProperty() + "=" + keyValuePair.getDefault() + " # "
                + comment + " [default: " + keyValuePair.getDefault() + "]\n";
    }

    @Override
    public String get(String namespace) {
        return configContents;
    }
}
