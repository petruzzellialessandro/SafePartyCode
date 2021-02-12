package com.gingerbread.SafeParty.ui.event.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.gingerbread.contagiapp.R;
import com.gingerbread.SafeParty.dataClasses.Evento;
import com.gingerbread.SafeParty.dataClasses.Utente;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class EventRequestAdapter extends ArrayAdapter<Utente> {
    private String IDEvento;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    public EventRequestAdapter(@NonNull Context context, int resource, ArrayList<Utente> utenti, String IDEvento) {
        super(context, resource);
        this.IDEvento = IDEvento;

    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Utente utente = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_user_request, parent, false);
        }

        TextView userNameSurname = convertView.findViewById(R.id.userNameSurname);
        ImageView userPhoto = convertView.findViewById(R.id.userPhoto);
        userNameSurname.setText(utente.getName());
        Glide
                .with(getContext())
                .load(utente.getPhotoUrl())
                .apply(RequestOptions.circleCropTransform())
                .into(userPhoto); //Your imageView variable
        Button accept = convertView.findViewById(R.id.accept_request);
        Button decline = convertView.findViewById(R.id.decline_request);
        View finalConvertView = convertView;
        accept.setOnClickListener(v -> {
            accept.setVisibility(View.GONE);
            decline.setVisibility(View.GONE);
            Accept(finalConvertView, utente);
        });
        decline.setOnClickListener(v -> {
            accept.setVisibility(View.GONE);
            decline.setVisibility(View.GONE);
            Decline(finalConvertView, utente);
        });
        return convertView;
    }
    private void Decline(View convertView, Utente utente){
        db.collection("Utenti").whereEqualTo("email", utente.getEmail()).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                String idUtente = task.getResult().getDocuments().get(0).getId();
                utente.removeFromRequest(this.IDEvento);
                db.collection("Eventi").document(IDEvento).get().addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()){
                        Evento evento = task1.getResult().toObject(Evento.class);
                        evento.removRequest(idUtente);
                        new Thread(()->{
                            db.collection("Utenti").document(idUtente).set(utente).addOnCompleteListener(task2 ->
                                    db.collection("Eventi").document(IDEvento).set(evento).addOnCompleteListener(task21 -> {
                                        convertView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.red));
                                    }));
                        }).start();
                    }
                });

            }

        });
    }

    private void Accept(View convertView, Utente utente){
        db.collection("Utenti").whereEqualTo("email", utente.getEmail()).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                String idUtente = task.getResult().getDocuments().get(0).getId();
                utente.addEventiPartecipati(this.IDEvento);
                utente.removeFromRequest(this.IDEvento);
                db.collection("Eventi").document(IDEvento).get().addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()){
                        Evento evento = task1.getResult().toObject(Evento.class);
                        evento.removRequest(idUtente);
                        evento.addPartecipante(idUtente);
                        new Thread(()->{
                            db.collection("Utenti").document(idUtente).set(utente).addOnCompleteListener(task2 ->
                                    db.collection("Eventi").document(IDEvento).set(evento).addOnCompleteListener(task21 -> {
                                        convertView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryColor));
                            }));
                        }).start();
                    }
                });

            }

        });
    }
}
