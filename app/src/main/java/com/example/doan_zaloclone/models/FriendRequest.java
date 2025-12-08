package com.example.doan_zaloclone.models;

public class FriendRequest {
    private String id;
    private String name;
    private String email;
    private Status status;

    public enum Status {
        PENDING, ACCEPTED, REJECTED
    }

    public FriendRequest(String id, String name, String email, Status status) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
