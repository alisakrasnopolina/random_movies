package com.example.random_movie.friends.model;

import java.io.Serializable;
import java.util.Objects;

public class FriendSessionMovie implements Serializable {
    private int movieId;
    private String title;
    private String posterUrl;
    private String genre;
    private int year;
    private int runtimeMin;
    private double ratingImdb;
    private boolean matched;

    public boolean isMatched() {
        return matched;
    }

    public FriendSessionMovie setMatched(boolean matched) {
        this.matched = matched;
        return this;
    }

    public FriendSessionMovie() {
    }

    public FriendSessionMovie(
            int movieId,
            String title,
            String posterUrl,
            String genre,
            int year,
            int runtimeMin,
            double ratingImdb
    ) {
        this.movieId = movieId;
        this.title = title;
        this.posterUrl = posterUrl;
        this.genre = genre;
        this.year = year;
        this.runtimeMin = runtimeMin;
        this.ratingImdb = ratingImdb;
    }

    public int getMovieId() {
        return movieId;
    }

    public FriendSessionMovie setMovieId(int movieId) {
        this.movieId = movieId;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public FriendSessionMovie setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public FriendSessionMovie setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
        return this;
    }

    public String getGenre() {
        return genre;
    }

    public FriendSessionMovie setGenre(String genre) {
        this.genre = genre;
        return this;
    }

    public int getYear() {
        return year;
    }

    public FriendSessionMovie setYear(int year) {
        this.year = year;
        return this;
    }

    public int getRuntimeMin() {
        return runtimeMin;
    }

    public FriendSessionMovie setRuntimeMin(int runtimeMin) {
        this.runtimeMin = runtimeMin;
        return this;
    }

    public double getRatingImdb() {
        return ratingImdb;
    }

    public FriendSessionMovie setRatingImdb(double ratingImdb) {
        this.ratingImdb = ratingImdb;
        return this;
    }

    @Override
    public String toString() {
        return "FriendSessionMovie{" +
                "movieId=" + movieId +
                ", title='" + title + '\'' +
                ", posterUrl='" + posterUrl + '\'' +
                ", genre='" + genre + '\'' +
                ", year=" + year +
                ", runtimeMin=" + runtimeMin +
                ", ratingImdb=" + ratingImdb +
                ", matched=" + matched +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FriendSessionMovie)) return false;
        FriendSessionMovie that = (FriendSessionMovie) o;
        return movieId == that.movieId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(movieId);
    }
}