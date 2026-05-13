package com.example.random_movie;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.widget.ImageView;

import com.example.random_movie.BuildConfig;
import com.example.random_movie.auth.ApiClient;
import com.example.random_movie.auth.LogoutHelper;
import com.example.random_movie.auth.SessionManager;
import com.example.random_movie.data.repository.WatchedRepository;
import com.google.android.material.textview.MaterialTextView;
import com.example.random_movie.friends.model.FriendUser;

import java.util.ArrayList;
import java.util.List;
import com.google.android.material.card.MaterialCardView;

import com.example.random_movie.friends.FriendsRepository;
import com.example.random_movie.friends.model.FriendUser;

import java.util.List;
import java.util.ArrayList;

import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.bumptech.glide.Glide;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.ByteArrayOutputStream;

import android.graphics.Matrix;
import androidx.exifinterface.media.ExifInterface;

import com.example.random_movie.profile.ProfileStatsRepository;


/**
 * @brief Главный экран профиля пользователя.
 *
 * Activity отображает основные данные пользователя, обеспечивает переход
 * к редактированию профиля, навигацию между разделами приложения
 * и выход из учетной записи.
 */
public class MainActivity extends BaseActivity {

    /** Текстовое поле с именем пользователя. */
    private TextView profileName;

    /** Текстовое поле с email пользователя. */
    private TextView profileEmail;

    /** Кнопка перехода к редактированию профиля. */
    private Button editProfile;

    /** Кнопка выхода из аккаунта. */
    private Button logoutButton;

    /** Менеджер локальной сессии пользователя. */
    private SessionManager sessionManager;

    /** Имя пользователя. */
    private String name;

    /** Email пользователя. */
    private String email;

    /** Идентификатор пользователя. */
    private String userId;
    private TextView watchedCountText;
    private TextView friendSessionsCountText;
    private TextView recommendationsCountText;
    private View openAllFriendsButton;
    private WatchedRepository watchedRepository;
    private LinearLayout friendsPreviewContainer;
    private FriendsRepository friendsRepository;
    private ImageView profileAvatar;
    private ActivityResultLauncher<String> avatarPickerLauncher;
    private ProfileStatsRepository profileStatsRepository;

    /**
     * @brief Инициализирует главный экран профиля.
     *
     * Проверяет наличие access token, загружает данные пользователя из SessionManager,
     * настраивает нижнюю навигацию, кнопку выхода и кнопку редактирования профиля.
     *
     * @param savedInstanceState сохраненное состояние Activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        profileName = findViewById(R.id.profileName);
        profileEmail = findViewById(R.id.profileEmail);
        profileAvatar = findViewById(R.id.profileAvatar);
        editProfile = findViewById(R.id.edit_button);
        logoutButton = findViewById(R.id.exit_button);

        watchedCountText = findViewById(R.id.watchedCountText);
        friendSessionsCountText = findViewById(R.id.friendSessionsCountText);
        recommendationsCountText = findViewById(R.id.recommendationsCountText);

        openAllFriendsButton = findViewById(R.id.openAllFriendsButton);

        friendsPreviewContainer = findViewById(R.id.friendsPreviewContainer);

        avatarPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        uploadAvatar(uri);
                    }
                }
        );

        if (friendSessionsCountText != null) friendSessionsCountText.setText("0");
        if (recommendationsCountText != null) recommendationsCountText.setText("0");
        if (watchedCountText != null) watchedCountText.setText("0");
        if (profileAvatar != null) {
            profileAvatar.setOnClickListener(v -> avatarPickerLauncher.launch("image/*"));
        }

        sessionManager = new SessionManager(this);
        watchedRepository = new WatchedRepository(this);
        friendsRepository = new FriendsRepository(this);
        profileStatsRepository = new ProfileStatsRepository(this);

        // если нет токена — сразу на логин
        if (sessionManager.getAccessToken() == null || sessionManager.getAccessToken().isEmpty()) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        userId = sessionManager.getUserId();
        name = sessionManager.getDisplayName();
        email = sessionManager.getEmail();

        showAllUserData();

        NavigationBar.setup(this, R.id.home);

        setupFriendsPreview();



        logoutButton.setOnClickListener(v -> {
            logoutButton.setEnabled(false);
            LogoutHelper.logout(
                    MainActivity.this,
                    () -> Toast.makeText(MainActivity.this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show()
            );
        });

        editProfile.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, EditProfileActivity.class);
            intent.putExtra("userID", userId);
            intent.putExtra("name", name);
            intent.putExtra("email", email);
            intent.putExtra("password", "");
            startActivity(intent);
        });
    }

    /**
     * @brief Обновляет отображаемые данные пользователя при возврате на экран.
     */
    @Override
    protected void onStart() {
        super.onStart();

        userId = sessionManager.getUserId();
        name = sessionManager.getDisplayName();
        email = sessionManager.getEmail();

        showAllUserData();
        loadProfileStats();
    }

    /**
     * @brief Отображает имя и email пользователя на экране профиля.
     */
    private void showAllUserData() {
        profileName.setText(name != null ? name : "");
        profileEmail.setText(email != null ? email : "");

        String avatarUrl = sessionManager.getAvatarUrl();

        if (profileAvatar != null) {
            profileAvatar.clearColorFilter();

            if (avatarUrl != null
                    && !avatarUrl.trim().isEmpty()
                    && !"null".equalsIgnoreCase(avatarUrl.trim())) {

                Glide.with(this)
                        .load(avatarUrl)
                        .placeholder(R.drawable.icon_user)
                        .error(R.drawable.icon_user)
                        .circleCrop()
                        .into(profileAvatar);
            } else {
                profileAvatar.setImageResource(R.drawable.icon_user);
            }
        }
    }

    private void loadProfileStats() {
        if (profileStatsRepository == null) return;

        profileStatsRepository.getMyStats(new ProfileStatsRepository.Callback() {
            @Override
            public void onSuccess(ProfileStatsRepository.ProfileStats stats) {
                runOnUiThread(() -> {
                    if (watchedCountText != null) {
                        watchedCountText.setText(String.valueOf(stats.watchedCount));
                    }

                    if (friendSessionsCountText != null) {
                        friendSessionsCountText.setText(String.valueOf(stats.finishedSessionsCount));
                    }

                    if (recommendationsCountText != null) {
                        recommendationsCountText.setText(String.valueOf(stats.recommendationsCount));
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    if (watchedCountText != null) watchedCountText.setText("0");
                    if (friendSessionsCountText != null) friendSessionsCountText.setText("0");
                    if (recommendationsCountText != null) recommendationsCountText.setText("0");
                });
            }
        });
    }

    private void setupFriendsPreview() {
        if (openAllFriendsButton != null) {
            openAllFriendsButton.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, FriendsActivity.class);
                intent.putExtra("mode", "view_only");
                startActivity(intent);
            });
        }

        loadFriendsPreview();
    }

    private void loadFriendsPreview() {
        if (friendsPreviewContainer == null) return;

        friendsRepository.getMyFriends(new FriendsRepository.Callback<List<FriendUser>>() {
            @Override
            public void onSuccess(List<FriendUser> friends) {
                runOnUiThread(() -> renderFriendsPreview(friends));
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> renderFriendsPreview(new ArrayList<>()));
            }
        });
    }

    private void renderFriendsPreview(List<FriendUser> friends) {
        if (friendsPreviewContainer == null) return;

        friendsPreviewContainer.removeAllViews();

        if (friends == null || friends.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("Пока нет друзей");
            emptyText.setTextColor(ContextCompat.getColor(this, R.color.textColor_subtitles));
            friendsPreviewContainer.addView(emptyText);
            return;
        }

        int limit = Math.min(3, friends.size());

        for (int i = 0; i < limit; i++) {
            friendsPreviewContainer.addView(createFriendPreviewCard(friends.get(i)));
        }
    }

    private View createFriendPreviewCard(FriendUser friend) {
        MaterialCardView card = new MaterialCardView(this);

        int cardWidth = getFriendPreviewCardWidth();

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                cardWidth,
                dp(122)
        );
        cardParams.setMargins(0, 0, dp(8), 0);
        card.setLayoutParams(cardParams);

        card.setRadius(dp(10));
        card.setCardElevation(0);
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(androidx.core.content.ContextCompat.getColor(this, R.color.textColor_subtitles));
        card.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.transparent));
        card.setClickable(true);
        card.setFocusable(true);

        LinearLayout content = new LinearLayout(this);
        content.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(10), dp(10), dp(10), dp(10));

        ImageView avatar = new ImageView(this);
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        avatar.setLayoutParams(avatarParams);
        if (friend.getAvatarUrl() != null && !friend.getAvatarUrl().isEmpty()) {
            Glide.with(this)
                    .load(friend.getAvatarUrl())
                    .placeholder(R.drawable.icon_user)
                    .circleCrop()
                    .into(avatar);
        } else {
            avatar.setImageResource(R.drawable.icon_user);
        }

        MaterialTextView name = new MaterialTextView(this);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        nameParams.setMargins(0, dp(8), 0, 0);
        name.setLayoutParams(nameParams);
        name.setText(friend.getName());
        name.setTextSize(16);
        name.setMaxLines(1);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        name.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.textColor));

        content.addView(avatar);
        content.addView(name);
        card.addView(content);

        card.setOnClickListener(v -> openFriendPublicProfile(
                friend.getId(),
                friend.getName(),
                friend.getAvatarUrl()
        ));

        return card;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private int getFriendPreviewCardWidth() {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;

        int pageHorizontalPadding = dp(32);

        int gapsBetweenThreeCards = dp(16);

        return (screenWidth - pageHorizontalPadding - gapsBetweenThreeCards) / 3;
    }

    private int getColorFromAttr(int attr) {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    private void openFriendPublicProfile(String userId, String name, String avatarUrl) {
        Intent intent = new Intent(MainActivity.this, FriendPublicProfileActivity.class);
        intent.putExtra("user_id", userId);
        intent.putExtra("name", name);
        intent.putExtra("avatar_url", avatarUrl);
        startActivity(intent);
    }

    private void uploadAvatar(Uri uri) {
        new Thread(() -> {
            try {
                String mimeType = getContentResolver().getType(uri);
                if (mimeType == null || !mimeType.startsWith("image/")) {
                    mimeType = "image/jpeg";
                }

                String extension = ".jpg";
                if ("image/png".equals(mimeType)) {
                    extension = ".png";
                } else if ("image/webp".equals(mimeType)) {
                    extension = ".webp";
                }

                String fileName = "avatar" + extension;

                byte[] bytes = compressAvatarFromUri(uri);

                RequestBody fileBody = RequestBody.create(
                        bytes,
                        MediaType.parse("image/jpeg")
                );

                MultipartBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", "avatar.jpg", fileBody)
                        .build();

                Request request = new Request.Builder()
                        .url(BuildConfig.API_BASE_URL + "/profile/me/avatar")
                        .post(requestBody)
                        .addHeader("accept", "application/json")
                        .build();

                try (Response response = ApiClient.get(this).newCall(request).execute()) {
                    String raw = response.body() != null ? response.body().string() : "{}";

                    if (!response.isSuccessful()) {
                        throw new RuntimeException("HTTP " + response.code() + ": " + raw);
                    }

                    JSONObject obj = new JSONObject(raw);
                    String avatarUrl = obj.optString("avatar_url", "");
                    Log.d("AvatarUpload", "avatarUrl = " + avatarUrl);
                    if (avatarUrl == null || avatarUrl.trim().isEmpty() || "null".equalsIgnoreCase(avatarUrl.trim())) {
                        throw new RuntimeException("Backend не вернул avatar_url");
                    }

                    sessionManager.saveAvatarUrl(avatarUrl);

                    runOnUiThread(() -> {
                        if (profileAvatar != null) {
                            profileAvatar.clearColorFilter();

                            Glide.with(MainActivity.this)
                                    .load(avatarUrl + "?t=" + System.currentTimeMillis())
                                    .placeholder(R.drawable.icon_user)
                                    .error(R.drawable.icon_user)
                                    .circleCrop()
                                    .into(profileAvatar);
                        }

                        Toast.makeText(MainActivity.this, "Аватар обновлён", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(
                                MainActivity.this,
                                "Ошибка загрузки аватара: " + (e.getMessage() != null ? e.getMessage() : ""),
                                Toast.LENGTH_LONG
                        ).show()
                );
            }
        }).start();
    }

    private byte[] readBytesFromUri(Uri uri) throws Exception {
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {

            if (inputStream == null) {
                throw new RuntimeException("Не удалось открыть файл");
            }

            byte[] data = new byte[8192];
            int nRead;

            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            return buffer.toByteArray();
        }
    }

    private byte[] compressAvatarFromUri(Uri uri) throws Exception {
        byte[] originalBytes = readBytesFromUri(uri);

        BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
        boundsOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.length, boundsOptions);

        int maxSize = 512;
        int sampleSize = 1;

        while (
                boundsOptions.outWidth / sampleSize > maxSize ||
                        boundsOptions.outHeight / sampleSize > maxSize
        ) {
            sampleSize *= 2;
        }

        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        decodeOptions.inSampleSize = sampleSize;

        Bitmap bitmap = BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.length, decodeOptions);

        if (bitmap == null) {
            throw new RuntimeException("Не удалось обработать изображение");
        }

        bitmap = rotateBitmapIfNeeded(bitmap, uri);

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        int quality = 85;
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output);

        while (output.size() > 1024 * 1024 && quality > 40) {
            output.reset();
            quality -= 10;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output);
        }

        bitmap.recycle();

        return output.toByteArray();
    }

    private Bitmap rotateBitmapIfNeeded(Bitmap bitmap, Uri uri) throws Exception {
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            if (inputStream == null) return bitmap;

            ExifInterface exif = new ExifInterface(inputStream);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
            );

            int rotationDegrees = 0;

            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                rotationDegrees = 90;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                rotationDegrees = 180;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                rotationDegrees = 270;
            }

            if (rotationDegrees == 0) {
                return bitmap;
            }

            Matrix matrix = new Matrix();
            matrix.postRotate(rotationDegrees);

            Bitmap rotatedBitmap = Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    bitmap.getWidth(),
                    bitmap.getHeight(),
                    matrix,
                    true
            );

            if (rotatedBitmap != bitmap) {
                bitmap.recycle();
            }

            return rotatedBitmap;
        }
    }
}