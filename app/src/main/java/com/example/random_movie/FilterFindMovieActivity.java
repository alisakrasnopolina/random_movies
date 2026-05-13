package com.example.random_movie;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.random_movie.friends.FriendSessionRepository;
import com.example.random_movie.friends.model.FriendSessionState;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.RangeSlider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FilterFindMovieActivity extends BaseActivity {

    private static final List<String> GENRES = Arrays.asList(
            "Все", "аниме", "биография", "боевик", "вестерн", "военный", "детектив", "детский",
            "для взрослых", "документальный", "драма", "игра", "история", "комедия", "концерт",
            "короткометражка", "криминал", "мелодрама", "музыка", "мультфильм", "мюзикл", "новости",
            "приключения", "реальное ТВ", "семейный", "спорт", "ток-шоу", "триллер", "ужасы",
            "фантастика", "фильм-нуар", "фэнтези", "церемония"
    );

    private MaterialTextView textGenreValue;
    private TextInputEditText editCountry;
    private RangeSlider sliderYear;
    private RangeSlider sliderRating;
    private MaterialTextView textYearRange;
    private MaterialTextView textRatingRange;
    private ChipGroup chipsCountries;
    private final Set<String> selectedCountries = new LinkedHashSet<>();
    private boolean friendMode;
    private String sessionId;
    private FriendSessionRepository friendRepository;

    private Button buttonApplyFilters;
    private boolean isApplyingFriendFilters = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filters);

        ImageButton buttonBack = findViewById(R.id.buttonBack);
        LinearLayout genreSelector = findViewById(R.id.genreSelector);
        textGenreValue = findViewById(R.id.textGenreValue);
        editCountry = findViewById(R.id.editCountry);
        chipsCountries = findViewById(R.id.chipsCountries);
        sliderYear = findViewById(R.id.sliderYear);
        sliderRating = findViewById(R.id.sliderRating);

        ensureRangeSliderValues();

        textYearRange = findViewById(R.id.textYearRange);
        textRatingRange = findViewById(R.id.textRatingRange);
        buttonApplyFilters = findViewById(R.id.buttonApplyFilters);
        Button buttonResetFilters = findViewById(R.id.buttonResetFilters);

        friendMode = "friend_session".equals(getIntent().getStringExtra("mode"));
        sessionId = getIntent().getStringExtra("session_id");
        friendRepository = new FriendSessionRepository(this);

        if (buttonApplyFilters != null) {
            buttonApplyFilters.setText(friendMode ? "Подобрать фильмы" : "Подобрать фильм");
        }

        if (friendMode && (sessionId == null || sessionId.trim().isEmpty())) {
            Toast.makeText(this, "Не передана сессия", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        readInitialFilters(getIntent());
        updateRangesText();

        genreSelector.setOnClickListener(v -> showGenreDialog());
        sliderYear.addOnChangeListener((slider, value, fromUser) -> updateRangesText());
        sliderRating.addOnChangeListener((slider, value, fromUser) -> updateRangesText());
        editCountry.setOnEditorActionListener((v, actionId, event) -> {
            boolean isDone = actionId == EditorInfo.IME_ACTION_DONE;
            boolean isEnter = event != null
                    && event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;
            if (isDone || isEnter) {
                addCountryFromInput();
                return true;
            }
            return false;
        });

        buttonBack.setOnClickListener(v -> {
            if (friendMode) {
                Intent intent = new Intent(this, FriendsActivity.class);
                startActivity(intent);
                finish();
            } else {
                finish();
            }
        });
        buttonResetFilters.setOnClickListener(v -> resetFilters());
        buttonApplyFilters.setOnClickListener(v -> applySelectedFilters());
    }

    @Override
    protected boolean shouldPollFriendInvites() {
        return false;
    }

    private void readInitialFilters(Intent source) {
        String genre = source.getStringExtra("genre");
        if (genre != null && !genre.trim().isEmpty()) {
            textGenreValue.setText(genre);
        }

        String country = source.getStringExtra("country");
        if (country != null) {
            selectedCountries.add(country);
        }

        if (source.hasExtra("year_from") && source.hasExtra("year_to")) {
            float from = source.getIntExtra("year_from", 1950);
            float to = source.getIntExtra("year_to", 2026);
            sliderYear.setValues(from, to);
        }

        if (source.hasExtra("rating_from") && source.hasExtra("rating_to")) {
            float from = source.getFloatExtra("rating_from", 0f);
            float to = source.getFloatExtra("rating_to", 10f);
            sliderRating.setValues(from, to);
        }

        renderCountryChips();
    }

    private void showGenreDialog() {
        String[] items = GENRES.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("Выберите жанр")
                .setItems(items, (dialog, which) -> textGenreValue.setText(items[which]))
                .show();
    }

    private void resetFilters() {
        textGenreValue.setText("Все");
        editCountry.setText("");
        selectedCountries.clear();
        chipsCountries.removeAllViews();
        sliderYear.setValues(1950f, 2026f);
        sliderRating.setValues(0f, 10f);
        updateRangesText();
    }

    private void addCountryFromInput() {
        String country = editCountry.getText() != null ? editCountry.getText().toString().trim() : "";
        if (country.isEmpty()) return;
        selectedCountries.add(country);
        editCountry.setText("");
        renderCountryChips();
    }

    private void renderCountryChips() {
        chipsCountries.removeAllViews();
        for (String country : selectedCountries) {
            Chip chip = new Chip(this);
            chip.setText(country);
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> {
                selectedCountries.remove(country);
                renderCountryChips();
            });
            chipsCountries.addView(chip);
        }
    }

    private void updateRangesText() {
        List<Float> years = sliderYear.getValues();

        int yearFrom = 1950;
        int yearTo = 2026;

        if (years != null && years.size() >= 2) {
            yearFrom = Math.round(years.get(0));
            yearTo = Math.round(years.get(1));
        } else if (years != null && years.size() == 1) {
            yearFrom = Math.round(years.get(0));
            yearTo = 2026;
            sliderYear.setValues((float) yearFrom, (float) yearTo);
        } else {
            sliderYear.setValues(1950f, 2026f);
        }

        textYearRange.setText(String.format(Locale.getDefault(), "%d — %d", yearFrom, yearTo));

        List<Float> ratings = sliderRating.getValues();

        float ratingFrom = 0f;
        float ratingTo = 10f;

        if (ratings != null && ratings.size() >= 2) {
            ratingFrom = ratings.get(0);
            ratingTo = ratings.get(1);
        } else if (ratings != null && ratings.size() == 1) {
            ratingFrom = ratings.get(0);
            ratingTo = 10f;
            sliderRating.setValues(ratingFrom, ratingTo);
        } else {
            sliderRating.setValues(0f, 10f);
        }

        textRatingRange.setText(String.format(Locale.getDefault(), "%.1f — %.1f", ratingFrom, ratingTo));
    }

    private void applyFilters() {
        Intent intent = new Intent();

        String genre = textGenreValue.getText() != null ? textGenreValue.getText().toString().trim() : "";
        addCountryFromInput();

        List<Float> years = sliderYear.getValues();
        int yearFrom = Math.round(years.get(0));
        int yearTo = Math.round(years.get(1));

        List<Float> ratings = sliderRating.getValues();
        float ratingFrom = ratings.get(0);
        float ratingTo = ratings.get(1);

        if (!genre.isEmpty() && !"Все".equalsIgnoreCase(genre)) {
            intent.putExtra("genre", genre);
        }

        if (!selectedCountries.isEmpty()) {
            intent.putExtra("country", selectedCountries.iterator().next());
        }

        if (yearFrom != 1950 || yearTo != 2026) {
            intent.putExtra("year_from", yearFrom);
            intent.putExtra("year_to", yearTo);
        }

        if (Math.abs(ratingFrom - 0f) > 0.001f || Math.abs(ratingTo - 10f) > 0.001f) {
            intent.putExtra("rating_from", ratingFrom);
            intent.putExtra("rating_to", ratingTo);
        }

        setResult(200, intent);
        finish();
    }

    private void applySelectedFilters() {
        ensureRangeSliderValues();

        if (friendMode && isApplyingFriendFilters) {
            return;
        }

        MovieFilters filters = new MovieFilters();

        String genre = textGenreValue.getText() != null
                ? textGenreValue.getText().toString().trim()
                : "";

        addCountryFromInput();

        List<Float> years = sliderYear.getValues();
        int yearFrom = Math.round(years.get(0));
        int yearTo = Math.round(years.get(1));

        List<Float> ratings = sliderRating.getValues();
        float ratingFrom = ratings.get(0);
        float ratingTo = ratings.get(1);

        if (!genre.isEmpty() && !"Все".equalsIgnoreCase(genre)) {
            filters.genre = genre;
        }

        if (!selectedCountries.isEmpty()) {
            filters.country = selectedCountries.iterator().next();
        }

        if (yearFrom != 1950 || yearTo != 2026) {
            filters.yearFrom = yearFrom;
            filters.yearTo = yearTo;
        }

        if (Math.abs(ratingFrom - 0f) > 0.001f || Math.abs(ratingTo - 10f) > 0.001f) {
            filters.ratingFrom = ratingFrom;
            filters.ratingTo = ratingTo;
        }

        if (friendMode) {
            applyFriendFilters(filters);
        } else {
            Intent intent = new Intent();

            if (filters.genre != null) intent.putExtra("genre", filters.genre);
            if (filters.country != null) intent.putExtra("country", filters.country);
            if (filters.yearFrom != null) intent.putExtra("year_from", filters.yearFrom);
            if (filters.yearTo != null) intent.putExtra("year_to", filters.yearTo);
            if (filters.ratingFrom != null) intent.putExtra("rating_from", filters.ratingFrom);
            if (filters.ratingTo != null) intent.putExtra("rating_to", filters.ratingTo);

            setResult(200, intent);
            finish();
        }
    }

    private void applyFriendFilters(MovieFilters filters) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            Toast.makeText(this, "Не передан session_id", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isApplyingFriendFilters) {
            return;
        }

        isApplyingFriendFilters = true;

        if (buttonApplyFilters != null) {
            buttonApplyFilters.setEnabled(false);
            buttonApplyFilters.setText("Подбираем фильмы...");
        }

        friendRepository.applyFilters(sessionId, filters, new FriendSessionRepository.Callback<FriendSessionState>() {
            @Override
            public void onSuccess(FriendSessionState data) {
                runOnUiThread(() -> {
                    isApplyingFriendFilters = false;

                    Intent intent = new Intent(FilterFindMovieActivity.this, FriendMovieVoteActivity.class);
                    intent.putExtra("session_id", sessionId);
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> checkSessionAfterFiltersError(message));
            }
        });
    }

    private void checkSessionAfterFiltersError(String originalMessage) {
        friendRepository.state(sessionId, new FriendSessionRepository.Callback<FriendSessionState>() {
            @Override
            public void onSuccess(FriendSessionState data) {
                runOnUiThread(() -> {
                    isApplyingFriendFilters = false;

                    if (data != null
                            && FriendSessionState.STATUS_ACTIVE.equalsIgnoreCase(data.getStatus())
                            && data.getMovies() != null
                            && !data.getMovies().isEmpty()) {

                        Intent intent = new Intent(FilterFindMovieActivity.this, FriendMovieVoteActivity.class);
                        intent.putExtra("session_id", sessionId);
                        startActivity(intent);
                        finish();
                        return;
                    }

                    if (buttonApplyFilters != null) {
                        buttonApplyFilters.setEnabled(true);
                        buttonApplyFilters.setText("Подобрать фильмы");
                    }

                    Toast.makeText(
                            FilterFindMovieActivity.this,
                            "Ошибка фильтров: " + originalMessage,
                            Toast.LENGTH_LONG
                    ).show();
                });
            }

            @Override
            public void onError(String stateError) {
                runOnUiThread(() -> {
                    isApplyingFriendFilters = false;

                    if (buttonApplyFilters != null) {
                        buttonApplyFilters.setEnabled(true);
                        buttonApplyFilters.setText("Подобрать фильмы");
                    }

                    Toast.makeText(
                            FilterFindMovieActivity.this,
                            "Ошибка фильтров: " + originalMessage,
                            Toast.LENGTH_LONG
                    ).show();
                });
            }
        });
    }

    private void ensureRangeSliderValues() {
        if (sliderYear.getValues() == null || sliderYear.getValues().size() < 2) {
            sliderYear.setValues(1950f, 2026f);
        }

        if (sliderRating.getValues() == null || sliderRating.getValues().size() < 2) {
            sliderRating.setValues(0f, 10f);
        }
    }
}