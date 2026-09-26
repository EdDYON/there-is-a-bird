package EdDYON.guaniao.client.config;

import java.util.List;
import java.util.Set;

/** UI grouping only: the configuration and its server-side permissions stay unchanged. */
enum BirdConfigCategory {
    GENERAL("general", "scope", "april_fools_mode", "sparrow_tide_mode", "bird_death_guilt_messages"),
    ECOLOGY("ecology", "enabled", "natural_spawning", "sky_bird_ecology", "colonial_mode",
            "spawn_multiplier", "min_group", "max_group", "enable_migration", "migration_interval_ticks", "migration_radius"),
    INTERACTION("interaction", "pet_bird_commands", "seagull_stealing", "crow_item_safety", "birds_pass_through_leaves",
            "seagull_steal_cooldown", "seagull_concurrent_targets", "flock_radius", "flock_max_members", "owner_teleport_distance"),
    NESTS("nests", "natural_crow_nests", "crow_nest_generation_multiplier", "crows_store_treasures",
            "crow_nest_search_distance", "max_crow_nest_treasures", "crows_claim_player_nests"),
    SOUND("sound", "sound_multiplier", "ambient_sound_cooldown"),
    DROPPINGS("droppings", "dropping_multiplier", "natural_droppings", "max_droppings", "dropping_radius",
            "dropping_area_min_seconds", "dropping_area_max_seconds", "dropping_lifetime_min_minutes",
            "dropping_lifetime_max_minutes", "dropping_pressure_plate_pulse", "dropping_pressure_plate_pulse_ticks"),
    PHOTOS("photos", "photo_uploads", "photo_uploads_operator_only", "photo_uploads_whitelist_only",
            "max_photos_per_player", "max_photo_storage_player", "max_photos_per_world", "max_photo_storage_world",
            "photo_trash_retention_days", "max_photo_downloads", "photo_download_kib_tick"),
    PERFORMANCE("performance", "max_birds", "max_wild_birds_region", "population_region_chunks", "wild_bird_despawn_ticks",
            "flyby_bird_lifetime_ticks", "flock_refresh_ticks", "habitat_cache_ticks", "bird_scan_budget",
            "max_wild_nearby", "food_scan_interval", "threat_scan_interval");

    static final List<BirdConfigCategory> SPECIES = List.of(ECOLOGY, INTERACTION, SOUND, DROPPINGS, PERFORMANCE);
    final String id;
    private final Set<String> keys;

    BirdConfigCategory(String id, String... keys) {
        this.id = id;
        this.keys = Set.of(keys);
    }

    boolean contains(String key) { return this.keys.contains(key); }
    String translationKey() { return "gui.guaniao.bird_config.category." + this.id; }
}
