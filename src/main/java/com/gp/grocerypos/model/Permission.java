package com.gp.grocerypos.model;

/**
 * Represents a single system permission (e.g. SALE_CREATE, PRODUCT_EDIT).
 * Permissions are assigned to roles via the role_permissions table.
 */
public class Permission {

    private int    id;
    private String name;
    private String description;
    private String module;

    public Permission() {}

    public Permission(int id, String name, String description, String module) {
        this.id          = id;
        this.name        = name;
        this.description = description;
        this.module      = module;
    }

    public int    getId()          { return id; }
    public void   setId(int id)    { this.id = id; }

    public String getName()             { return name; }
    public void   setName(String name)  { this.name = name; }

    public String getDescription()                   { return description; }
    public void   setDescription(String description) { this.description = description; }

    public String getModule()              { return module; }
    public void   setModule(String module) { this.module = module; }

    @Override
    public String toString() {
        return "Permission{name='" + name + "', module='" + module + "'}";
    }
}
