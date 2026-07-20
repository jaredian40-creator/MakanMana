package com.example.makanmana.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.makanmana.R;
import com.example.makanmana.data.PhotoRepository;
import com.example.makanmana.model.Photo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Gallery of {@link Photo} cards: image + rating + caption + place & date. */
public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.VH> {

    public interface OnPhotoClick {
        void onClick(Photo photo);
    }

    private final List<Photo> photos;
    private final OnPhotoClick listener;
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());

    public PhotoAdapter(List<Photo> photos) {
        this(photos, null);
    }

    public PhotoAdapter(List<Photo> photos, OnPhotoClick listener) {
        this.photos = photos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_photo_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Photo p = photos.get(position);

        byte[] bytes = PhotoRepository.decodeGlideModel(p.imageBase64);
        Glide.with(h.itemView.getContext())
                .load(bytes)
                .placeholder(R.drawable.ic_food_placeholder)
                .into(h.img);

        h.tvRating.setText(p.rating > 0
                ? String.format(Locale.getDefault(), "⭐ %.1f", p.rating)
                : "⭐ Not rated");

        if (p.caption != null && !p.caption.isEmpty()) {
            h.tvCaption.setVisibility(View.VISIBLE);
            h.tvCaption.setText(p.caption);
        } else {
            h.tvCaption.setVisibility(View.GONE);
        }

        StringBuilder meta = new StringBuilder();
        if (p.placeName != null && !p.placeName.isEmpty()) meta.append(p.placeName);
        if (p.createdAt > 0) {
            if (meta.length() > 0) meta.append("  •  ");
            meta.append(dateFmt.format(new Date(p.createdAt)));
        }
        h.tvMeta.setText(meta.toString());

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(p);
        });
    }

    @Override
    public int getItemCount() {
        return photos.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView img;
        final TextView tvRating, tvCaption, tvMeta;

        VH(@NonNull View v) {
            super(v);
            img = v.findViewById(R.id.imgPhoto);
            tvRating = v.findViewById(R.id.tvRating);
            tvCaption = v.findViewById(R.id.tvCaption);
            tvMeta = v.findViewById(R.id.tvMeta);
        }
    }
}
