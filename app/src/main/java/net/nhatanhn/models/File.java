package net.nhatanhn.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "file")
public class File {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String type;
    private String name;
    private int accessLevel;

    public String getType() {
        return type;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getAccessLevel() {
        return accessLevel;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAccessLevel(int accessLevel) {
        this.accessLevel = accessLevel;
    }

    @Override
    public String toString() {
        return String.format("{id: %d, type: %s, name: %s, accessLevel: %d}", id, type, name, accessLevel);
    }
}
