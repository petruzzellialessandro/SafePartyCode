package com.gingerbread.SafeParty.ui.event.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.gingerbread.SafeParty.dataClasses.Utente;
import com.gingerbread.contagiapp.R;

import java.util.ArrayList;

public class EventUserAdapter extends ArrayAdapter<Utente> {
    public EventUserAdapter(@NonNull Context context, int resource, ArrayList<Utente> utenti) {
        super(context, resource);

    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Utente utente = getItem(position);
        Log.d("Utente", utente.getName());
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_user_card, parent, false);
        }

        TextView userNameSurname = convertView.findViewById(R.id.userNameSurname);
        ImageView userPhoto = convertView.findViewById(R.id.userPhoto);
        userNameSurname.setText(utente.getName());
        Glide
                .with(getContext())
                .load(utente.getPhotoUrl())
                .apply(RequestOptions.circleCropTransform())
                .into(userPhoto); //Your imageView variable

        return convertView;
    }
}
