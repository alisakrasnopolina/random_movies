package com.example.random_movie.friends;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.random_movie.R;

import java.util.ArrayList;
import java.util.List;

public class FriendResultAdapter extends RecyclerView.Adapter<FriendResultAdapter.ResultViewHolder> {

    public static class ResultMovieItem {
        public int movieId;
        public String title;
        public String subtitle;
        public String meta;
        public boolean matched;

        public ResultMovieItem(int movieId, String title, String subtitle, String meta, boolean matched) {
            this.movieId = movieId;
            this.title = title;
            this.subtitle = subtitle;
            this.meta = meta;
            this.matched = matched;
        }
    }

    public interface OnMovieClickListener {
        void onMovieClick(int movieId);
    }

    private final List<ResultMovieItem> items = new ArrayList<>();
    private final OnMovieClickListener clickListener;

    public FriendResultAdapter(@NonNull OnMovieClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void submitList(List<ResultMovieItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_card_of_liked_movie, parent, false);
        return new ResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResultViewHolder holder, int position) {
        ResultMovieItem item = items.get(position);

        holder.title.setText(item.title != null ? item.title : "");
        holder.subtitle.setText(item.subtitle != null ? item.subtitle : "");
        holder.meta.setText(item.meta != null ? item.meta : "");

//        if (holder.unmatchedOverlay != null) {
//            holder.unmatchedOverlay.setVisibility(item.matched ? View.GONE : View.VISIBLE);
//        }

        holder.itemView.setOnClickListener(v -> clickListener.onMovieClick(item.movieId));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ResultViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView subtitle;
        final TextView meta;
//        final FrameLayout unmatchedOverlay;

        ResultViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.movie_name);
            subtitle = itemView.findViewById(R.id.movie_genre);
            meta = itemView.findViewById(R.id.movie_length);
//            unmatchedOverlay = itemView.findViewById(R.id.unmatchedOverlay);
        }
    }
}