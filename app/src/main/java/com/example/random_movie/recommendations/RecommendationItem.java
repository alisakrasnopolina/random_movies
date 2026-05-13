package com.example.random_movie.recommendations;

public class RecommendationItem {
    public String id;

    public String userId;
    public String userDisplayName;
    public String userAvatarUrl;

    public int movieId;
    public String movieTitle;
    public int movieYear;
    public String movieGenre;
    public String moviePosterUrl;
    public int movieRuntimeMin;
    public double movieRatingImdb;

    public int userRating;
    public String createdAt;
}