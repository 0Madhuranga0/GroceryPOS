package com.gp.grocerypos.model;

/**
 * Represents a single application configuration entry stored in the
 * {@code settings} table. Settings are key-value pairs grouped by module.
 * <p>
 * All values are stored as strings in the database. Typed accessors
 * ({@link #getValueAsInt()}, {@link #getValueAsBoolean()}, etc.) handle
 * conversion at the application layer.
 */
public class Setting {

    private int    id;
    private String key;
    private String value;
    private String group;
    private String description;

    public Setting() {}

    public Setting(String key, String value) {
        this.key   = key;
        this.value = value;
    }

    public Setting(String key, String value, String group, String description) {
        this.key         = key;
        this.value       = value;
        this.group       = group;
        this.description = description;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public int    getId()              { return id; }
    public void   setId(int id)        { this.id = id; }

    public String getKey()             { return key; }
    public void   setKey(String key)   { this.key = key; }

    public String getValue()               { return value; }
    public void   setValue(String value)   { this.value = value; }

    public String getGroup()               { return group; }
    public void   setGroup(String group)   { this.group = group; }

    public String getDescription()                     { return description; }
    public void   setDescription(String description)   { this.description = description; }

    // ── Typed accessors ───────────────────────────────────────────────────────

    /**
     * Returns the value as an int, or {@code defaultValue} if null/unparseable.
     */
    public int getValueAsInt(int defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try { return Integer.parseInt(value.trim()); }
        catch (NumberFormatException e) { return defaultValue; }
    }

    /**
     * Returns the value as a boolean. Treats "true" (case-insensitive) as true.
     */
    public boolean getValueAsBoolean(boolean defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        return Boolean.parseBoolean(value.trim());
    }

    /**
     * Returns the value as a double, or {@code defaultValue} if null/unparseable.
     */
    public double getValueAsDouble(double defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try { return Double.parseDouble(value.trim()); }
        catch (NumberFormatException e) { return defaultValue; }
    }

    /**
     * Returns the trimmed string value, or {@code defaultValue} if null/blank.
     */
    public String getValueOrDefault(String defaultValue) {
        return (value != null && !value.isBlank()) ? value.trim() : defaultValue;
    }

    @Override
    public String toString() {
        return "Setting{key='" + key + "', value='" + value + "', group='" + group + "'}";
    }
}
