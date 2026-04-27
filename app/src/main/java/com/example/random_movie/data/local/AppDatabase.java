package com.example.random_movie.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.random_movie.data.local.dao.CachedMovieDao;
import com.example.random_movie.data.local.dao.FavoritesDao;
import com.example.random_movie.data.local.dao.PendingActionDao;
import com.example.random_movie.data.local.dao.WatchLaterDao;
import com.example.random_movie.data.local.dao.WatchedDao;
import com.example.random_movie.data.local.entity.CachedMovieEntity;
import com.example.random_movie.data.local.entity.FavoriteEntity;
import com.example.random_movie.data.local.entity.PendingActionEntity;
import com.example.random_movie.data.local.entity.WatchLaterEntity;
import com.example.random_movie.data.local.entity.WatchedEntity;

@Database(
        entities = {
                CachedMovieEntity.class,
                FavoriteEntity.class,
                WatchLaterEntity.class,
                WatchedEntity.class,
                PendingActionEntity.class
        },
        version = 2,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract CachedMovieDao cachedMovieDao();
    public abstract FavoritesDao favoritesDao();
    public abstract WatchLaterDao watchLaterDao();
    public abstract WatchedDao watchedDao();
    public abstract PendingActionDao pendingActionDao();

    private static volatile AppDatabase INSTANCE;

    // Если ранее была версия 1 без watch_later/watched/pending_actions — добавляем таблицы
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `watch_later` (" +
                    "`userId` TEXT NOT NULL, " +
                    "`movieId` INTEGER NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL, " +
                    "`synced` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`userId`, `movieId`))");

            db.execSQL("CREATE TABLE IF NOT EXISTS `watched` (" +
                    "`userId` TEXT NOT NULL, " +
                    "`movieId` INTEGER NOT NULL, " +
                    "`watchedAt` INTEGER NOT NULL, " +
                    "`synced` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`userId`, `movieId`))");

            db.execSQL("CREATE TABLE IF NOT EXISTS `pending_actions` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`userId` TEXT NOT NULL, " +
                    "`actionType` TEXT NOT NULL, " +
                    "`movieId` INTEGER NOT NULL, " +
                    "`payloadJson` TEXT NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL, " +
                    "`retryCount` INTEGER NOT NULL, " +
                    "`status` TEXT NOT NULL)");

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_pending_actions_status_createdAt` " +
                    "ON `pending_actions` (`status`, `createdAt`)");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "random_movies.db"
                            )
                            .addMigrations(MIGRATION_1_2)
                            // Для dev можно оставить, для прод лучше убрать
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}