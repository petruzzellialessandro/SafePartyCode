package com.gingerbread.SafeParty.ui.event;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.gingerbread.SafeParty.ui.event.adapter.EventRequestAdapter;
import com.gingerbread.contagiapp.R;
import com.gingerbread.SafeParty.dataClasses.Evento;
import com.gingerbread.SafeParty.dataClasses.Utente;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class EventRequest extends AppCompatActivity {
    private ListView request;
    private EventRequestAdapter eventRequestAdapter;
    private ArrayList<Utente> utenti;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    String IDEvento;
    Evento evento;
    ProgressBar progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_request);
        if (getIntent().getExtras() == null){
            finish();
        }
        IDEvento = getIntent().getExtras().getString("ID_Evento");
        request = findViewById(R.id.request);
        progressBar = findViewById(R.id.loadingRequest);
        request.setVisibility(View.GONE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        db.collection("Eventi").document(IDEvento).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                evento = task.getResult().toObject(Evento.class);
                setRequestView();
            }
        });
    }

    private void setRequestView() {
        utenti = new ArrayList<Utente>();
        eventRequestAdapter = new EventRequestAdapter(this, android.R.layout.simple_list_item_1, utenti, IDEvento);
        request.setAdapter(eventRequestAdapter);
        if (evento.getRichieste().size()>0){
            for (String idRichiedenti : evento.getRichieste()){
                db.collection("Utenti").document(idRichiedenti).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()){
                        Utente utente = task.getResult().toObject(Utente.class);
                        eventRequestAdapter.add(utente);
                        eventRequestAdapter.notifyDataSetChanged();
                        progressBar.setVisibility(View.GONE);
                        request.setVisibility(View.VISIBLE);
                    }
                });
            }
        }
        else
        {
            progressBar.setVisibility(View.GONE);
        }


    }
}