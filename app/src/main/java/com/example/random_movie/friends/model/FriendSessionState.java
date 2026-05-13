package com.example.random_movie.friends.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import java.util.HashMap;
import java.util.Map;
public class FriendSessionState implements Serializable {
    public static final String STATUS_INVITED = "INVITED";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_WAITING = "WAITING";
    public static final String STATUS_FINISHED = "FINISHED";
    public static final String STATUS_REJECTED = "REJECTED";

    private String sessionId;
    private String ownerUserId;
    private String friendUserId;
    private String status = STATUS_INVITED;
    private List<FriendSessionMovie> movies = new ArrayList<>();
    private int ownerProgress;
    private int friendProgress;
    private Map<String, Boolean> ownerVotes = new HashMap<>();
    private Map<String, Boolean> friendVotes = new HashMap<>();

    private String ownerDisplayName;
    private String ownerAvatarUrl;
    private String friendDisplayName;
    private String friendAvatarUrl;

    public FriendSessionState() {
    }

    public FriendSessionState(
            String sessionId,
            String ownerUserId,
            String friendUserId,
            String status,
            List<FriendSessionMovie> movies,
            int ownerProgress,
            int friendProgress
    ) {
        this.sessionId = sessionId;
        this.ownerUserId = ownerUserId;
        this.friendUserId = friendUserId;
        this.status = status;
        this.movies = (movies == null) ? new ArrayList<>() : movies;
        this.ownerProgress = ownerProgress;
        this.friendProgress = friendProgress;
    }

    public String getSessionId() {
        return sessionId;
    }

    public FriendSessionState setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public FriendSessionState setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
        return this;
    }

    public String getFriendUserId() {
        return friendUserId;
    }

    public FriendSessionState setFriendUserId(String friendUserId) {
        this.friendUserId = friendUserId;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public FriendSessionState setStatus(String status) {
        this.status = status;
        return this;
    }

    public List<FriendSessionMovie> getMovies() {
        return movies;
    }

    public FriendSessionState setMovies(List<FriendSessionMovie> movies) {
        this.movies = (movies == null) ? new ArrayList<>() : movies;
        return this;
    }

    public int getOwnerProgress() {
        return ownerProgress;
    }

    public FriendSessionState setOwnerProgress(int ownerProgress) {
        this.ownerProgress = ownerProgress;
        return this;
    }

    public int getFriendProgress() {
        return friendProgress;
    }

    public FriendSessionState setFriendProgress(int friendProgress) {
        this.friendProgress = friendProgress;
        return this;
    }

    public boolean isFinished() {
        return STATUS_FINISHED.equalsIgnoreCase(status);
    }

    public boolean isWaiting() {
        return STATUS_WAITING.equalsIgnoreCase(status);
    }

    @Override
    public String toString() {
        return "FriendSessionState{" +
                "sessionId='" + sessionId + '\'' +
                ", ownerUserId='" + ownerUserId + '\'' +
                ", friendUserId='" + friendUserId + '\'' +
                ", status='" + status + '\'' +
                ", movies=" + movies +
                ", ownerProgress=" + ownerProgress +
                ", friendProgress=" + friendProgress +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FriendSessionState)) return false;
        FriendSessionState that = (FriendSessionState) o;
        return Objects.equals(sessionId, that.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId);
    }

    public String getOwnerDisplayName() {
        return ownerDisplayName;
    }

    public void setOwnerDisplayName(String ownerDisplayName) {
        this.ownerDisplayName = ownerDisplayName;
    }

    public String getOwnerAvatarUrl() {
        return ownerAvatarUrl;
    }

    public void setOwnerAvatarUrl(String ownerAvatarUrl) {
        this.ownerAvatarUrl = ownerAvatarUrl;
    }

    public String getFriendDisplayName() {
        return friendDisplayName;
    }

    public void setFriendDisplayName(String friendDisplayName) {
        this.friendDisplayName = friendDisplayName;
    }

    public String getFriendAvatarUrl() {
        return friendAvatarUrl;
    }

    public void setFriendAvatarUrl(String friendAvatarUrl) {
        this.friendAvatarUrl = friendAvatarUrl;
    }

    public Map<String, Boolean> getOwnerVotes() {
        return ownerVotes;
    }

    public FriendSessionState setOwnerVotes(Map<String, Boolean> ownerVotes) {
        this.ownerVotes = ownerVotes != null ? ownerVotes : new HashMap<>();
        return this;
    }

    public Map<String, Boolean> getFriendVotes() {
        return friendVotes;
    }

    public FriendSessionState setFriendVotes(Map<String, Boolean> friendVotes) {
        this.friendVotes = friendVotes != null ? friendVotes : new HashMap<>();
        return this;
    }

    public int getOwnerLikesCount() {
        return countVotes(ownerVotes, true);
    }

    public int getOwnerDislikesCount() {
        return countVotes(ownerVotes, false);
    }

    public int getFriendLikesCount() {
        return countVotes(friendVotes, true);
    }

    public int getFriendDislikesCount() {
        return countVotes(friendVotes, false);
    }

    private int countVotes(Map<String, Boolean> votes, boolean expected) {
        if (votes == null) return 0;

        int count = 0;
        for (Boolean value : votes.values()) {
            if (value != null && value == expected) {
                count++;
            }
        }
        return count;
    }
}