package EdDYON.guaniao.config;

import java.util.LinkedHashMap;
import java.util.Map;

public class BirdConfigData {
    public BirdGlobalConfig global = new BirdGlobalConfig();
    public LinkedHashMap<String, BirdSpeciesConfig> birds = new LinkedHashMap<>();
    public transient BirdConfigScope storageScope = BirdConfigScope.GLOBAL;
    public transient boolean worldScopeAllowed;

    public BirdConfigData copy() {
        BirdConfigData copy = new BirdConfigData();
        copy.global = this.global == null ? new BirdGlobalConfig() : this.global.copy();
        copy.storageScope = BirdConfigScope.sanitize(this.storageScope);
        copy.worldScopeAllowed = this.worldScopeAllowed;
        copy.birds.clear();
        if (this.birds != null) {
            for (Map.Entry<String, BirdSpeciesConfig> entry : this.birds.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    copy.birds.put(entry.getKey(), entry.getValue().copy());
                }
            }
        }
        return copy;
    }
}
