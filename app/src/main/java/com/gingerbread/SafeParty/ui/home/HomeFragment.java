package com.gingerbread.SafeParty.ui.home;

import android.app.ActivityOptions;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import com.gingerbread.SafeParty.ui.event.EventActivity;
import com.gingerbread.SafeParty.ui.event.EventCreation;
import com.gingerbread.contagiapp.R;
import com.gingerbread.SafeParty.dataClasses.Evento;
import com.gingerbread.SafeParty.dataClasses.Utente;
import com.gingerbread.SafeParty.ui.event.adapter.EventCardAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;

public class HomeFragment extends Fragment {

    private TextView servizio;
    private EventCardAdapter eventCardAdapter;
    private ArrayList<Evento> listaEventi;
    private ArrayList<String> listaEventiID;
    private ListView event_inc;
    private Utente utente;
    private ProgressBar progressBar;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        servizio.setText(getString(R.string.service_status_deactivated));
                        break;
                    case BluetoothAdapter.STATE_ON:
                        servizio.setText(getString(R.string.service_status_activated));
                        break;
                }
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.N)
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        event_inc = root.findViewById(R.id.event_inc);
        servizio = root.findViewById(R.id.servizio);
        event_inc.setVisibility(View.GONE);
        progressBar = root.findViewById(R.id.loading);
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        getActivity().registerReceiver(broadcastReceiver, filter);
        FloatingActionButton newEvent = root.findViewById(R.id.floatingActionButton);
        newEvent.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EventCreation.class);
            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(getActivity());
            startActivity(intent, options.toBundle());
        });
        db.collection("Utenti").document(FirebaseAuth.getInstance().getUid()).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                utente = task.getResult().toObject(Utente.class);
                setUpList();
            }
        });

        return root;
    }

    private void setUpList() {
        ArrayList<String> noEvent = new ArrayList<>();
        listaEventi = new ArrayList<>();
        listaEventiID = new ArrayList<>();
        eventCardAdapter = new EventCardAdapter(getActivity().getApplicationContext(), android.R.layout.simple_list_item_1, listaEventi);
        if (utente.getEventiPartecipati().isEmpty()) {
            noEvent.add(getString(R.string.no_participate_event));
            ArrayAdapter<String> adapterEventi = new ArrayAdapter<String>(getActivity().getApplicationContext(), android.R.layout.simple_list_item_1, noEvent);
            event_inc.setAdapter(adapterEventi);
            event_inc.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        } else {
            event_inc.setAdapter(eventCardAdapter);
            db.collection("Eventi").whereArrayContains("partecipanti", FirebaseAuth.getInstance().getUid()).orderBy("data").get().addOnCompleteListener(task1 -> {
                if (task1.isSuccessful()) {
                    for (DocumentSnapshot documentSnapshot : task1.getResult().getDocuments()) {
                        Evento evento = documentSnapshot.toObject(Evento.class);
                        if (evento.getData().after(new Date())) {
                            listaEventi.add(evento);
                            listaEventiID.add(documentSnapshot.getId());
                            eventCardAdapter.add(evento);
                            eventCardAdapter.notifyDataSetChanged();
                            event_inc.setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.GONE);
                        }
                    }
                }
            });
            event_inc.setOnItemClickListener((parent, view, position, id) -> {
                Intent intent = new Intent(getActivity(), EventActivity.class);
                intent.putExtra("ID_Evento", listaEventiID.get(position));
                startActivity(intent);
            });
        }

    }

}