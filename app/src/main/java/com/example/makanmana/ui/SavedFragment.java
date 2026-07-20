package com.example.makanmana.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.makanmana.PlaceDetailActivity;
import com.example.makanmana.R;
import com.example.makanmana.adapter.PlaceCardAdapter;
import com.example.makanmana.data.FavouritesRepository;
import com.example.makanmana.model.PlaceItem;

import java.util.ArrayList;
import java.util.List;

/** Saved tab: lists the user's favourite places from Firestore. */
public class SavedFragment extends Fragment {

    private final List<PlaceItem> favourites = new ArrayList<>();
    private PlaceCardAdapter adapter;
    private TextView tvEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_saved, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvEmpty = view.findViewById(R.id.tvEmpty);

        RecyclerView rv = view.findViewById(R.id.rvSaved);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new PlaceCardAdapter(favourites, R.layout.item_place_card, this::openDetail);
        rv.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFavourites();
    }

    private void loadFavourites() {
        FavouritesRepository.getAll(new FavouritesRepository.ListCallback() {
            @Override
            public void onResult(List<PlaceItem> places) {
                if (!isAdded()) return;
                favourites.clear();
                favourites.addAll(places);
                adapter.notifyDataSetChanged();
                tvEmpty.setVisibility(places.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText("Couldn't load favourites: " + message);
            }
        });
    }

    private void openDetail(PlaceItem p) {
        Intent i = new Intent(requireContext(), PlaceDetailActivity.class);
        i.putExtra("PLACE_ID", p.placeId);
        startActivity(i);
    }
}
