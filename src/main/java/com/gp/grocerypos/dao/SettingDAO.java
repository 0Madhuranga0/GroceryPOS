package com.gp.grocerypos.dao;

import com.gp.grocerypos.model.Setting;

import java.util.List;
import java.util.Optional;

/**
 * Data access contract for application {@link Setting} entries.
 * <p>
 * Settings are keyed by a unique string key. All mutations go through
 * {@link #setValue(String, String)} or {@link #upsert(Setting)} — there
 * is never a need to delete settings, only update their values.
 */
public interface SettingDAO {

    Optional<Setting> findByKey(String key);

    /** Returns the raw string value for a key, or {@code defaultValue} if absent. */
    String getValue(String key, String defaultValue);

    /** Returns all settings ordered by group and key. */
    List<Setting> findAll();

    /** Returns all settings belonging to a specific group (e.g. "STORE", "TAX"). */
    List<Setting> findByGroup(String group);

    /**
     * Updates the value of an existing setting.
     * Throws {@link com.gp.grocerypos.exception.DatabaseException} if key not found.
     */
    void setValue(String key, String value);

    /**
     * Insert or update a setting (INSERT … ON DUPLICATE KEY UPDATE).
     * Safe to call whether the key exists or not.
     */
    void upsert(Setting setting);

    /**
     * Atomically increments a numeric setting and returns the new value.
     * Used for invoice/purchase/return number counters.
     * The key must hold a valid integer value.
     */
    int incrementAndGet(String key);
}
