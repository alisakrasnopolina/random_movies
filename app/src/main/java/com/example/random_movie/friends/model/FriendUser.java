package com.example.random_movie.friends.model;

import java.io.Serializable;
import java.util.Objects;

public class FriendUser implements Serializable {
    private String id;
    private String name;
    private String avatarUrl;

    public FriendUser() {
    }

    public FriendUser(String id, String name, String avatarUrl) {
        this.id = id;
        this.name = name;
        this.avatarUrl = avatarUrl;
    }

    public String getId() {
        return id;
    }

    public FriendUser setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public FriendUser setName(String name) {
        this.name = name;
        return this;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public FriendUser setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        return this;
    }

    @Override
    public String toString() {
        return "FriendUser{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", avatarUrl='" + avatarUrl + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FriendUser)) return false;
        FriendUser that = (FriendUser) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}