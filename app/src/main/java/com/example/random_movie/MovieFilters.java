package com.example.random_movie;

import java.io.Serializable;

public class MovieFilters implements Serializable {
    public String genre;
    public String country;
    public Integer yearFrom;
    public Integer yearTo;
    public Float ratingFrom;
    public Float ratingTo;

    public boolean isEmpty() {
        return isBlank(genre) && isBlank(country)
                && yearFrom == null && yearTo == null
                && ratingFrom == null && ratingTo == null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty() || "Все".equalsIgnoreCase(value.trim());
    }
}