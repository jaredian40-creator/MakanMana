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
import com.example.makanmana.model.PlaceItem;

import java.util.List;
import java.util.Locale;

/**
 * Renders a list of {@link PlaceItem} as cards. The item layout is passed in so the
 * same adapter powers both the horizontal home carousel and the vertical saved list.
 */
public class PlaceCardAdapter extends RecyclerView.Adapter<PlaceCardAdapter.VH> {

    public interface OnPlaceClick {
        void onClick(PlaceItem place);
    }

    private final List<PlaceItem> items;
    private final int layoutRes;
    private final OnPlaceClick listener;

    public PlaceCardAdapter(List<PlaceItem> items, int layoutRes, OnPlaceClick listener) {
        this.items = items;
        this.layoutRes = layoutRes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        PlaceItem p = items.get(position);
        h.tvName.setText(p.name);
        h.tvAddress.setText(p.address);

        StringBuilder meta = new StringBuilder();
        if (p.rating > 0) meta.append(String.format(Locale.getDefault(), "⭐ %.1f", p.rating));
        if (!p.priceLabel().isEmpty()) meta.append("  ").append(p.priceLabel());
        if (p.openNow) meta.append("  • Open now");
        h.tvMeta.setText(meta.toString());

        String url = p.photoUrl(h.itemView.getContext(), 500);
        if (url != null) {
            Glide.with(h.itemView.getContext())
                    .load(url)
                    .placeholder(R.drawable.ic_food_placeholder)
                    .into(h.img);
        } else {
            h.img.setImageResource(R.drawable.ic_food_placeholder);
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(p);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView img;
        final TextView tvName, tvMeta, tvAddress;

        VH(@NonNull View v) {
            super(v);
            img = v.findViewById(R.id.imgPlace);
            tvName = v.findViewById(R.id.tvName);
            tvMeta = v.findViewById(R.id.tvMeta);
            tvAddress = v.findViewById(R.id.tvAddress);
        }
    }
}
