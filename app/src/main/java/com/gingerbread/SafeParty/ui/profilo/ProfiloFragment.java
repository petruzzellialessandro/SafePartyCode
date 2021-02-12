package com.gingerbread.SafeParty.ui.profilo;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.evolve.backdroplibrary.BackdropContainer;
import com.gingerbread.SafeParty.ui.event.EventActivity;
import com.gingerbread.SafeParty.MainActivity;
import com.gingerbread.contagiapp.R;
import com.gingerbread.SafeParty.dataClasses.Contatto;
import com.gingerbread.SafeParty.dataClasses.Evento;
import com.gingerbread.SafeParty.dataClasses.Utente;
import com.gingerbread.SafeParty.ui.event.adapter.EventCardAdapter;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;

public class ProfiloFragment extends Fragment {

    private FirebaseUser user;
    private Utente utente;
    ListView eventiGestiti;
    ListView contattiRicevuti;
    TextView textViewStatus;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    BackdropContainer backdropContainer;
    ProgressBar progressBar;



    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        View root = inflater.inflate(R.layout.fragment_profilo, container, false);
        Toolbar toolbar = root.findViewById(R.id.toolbar);
        progressBar = root.findViewById(R.id.loadingProfile);
        int height= getActivity().getResources().getDimensionPixelSize(R.dimen.sneek_height);
        //Prendiamo il riferimento del backdrop e ne definiamo l'altezza
        backdropContainer =(BackdropContainer)root.findViewById(R.id.backdropcontainer);
        backdropContainer.attachToolbar(toolbar)
                .dropInterpolator(new LinearInterpolator())
                .dropHeight(height)
                .build();
        backdropContainer.setVisibility(View.GONE);
        eventiGestiti = root.findViewById(R.id.generated_event);
        //di base il backdrop si chiude
        backdropContainer.showBackview();
        //Se si preme sulla textview in cima al backdrop il backdtop si apre e si chiude
        TextView backdrop_Action = root.findViewById(R.id.backdrop_Action);
        backdrop_Action.setOnClickListener(v -> {
            if (backdrop_Action.getText().equals(getString(R.string.reduce))){
                backdropContainer.showBackview();
                backdrop_Action.setText(getText(R.string.expand));
            }
            else
            {
                backdropContainer.closeBackview();
                backdrop_Action.setText(getText(R.string.reduce));
            }
        });
        //Si gesticono le opzioni presenti nel menu della toolbar
        toolbar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {

                case R.id.action_update_room://per la modifica del profilo
                    startActivity(new Intent(getActivity(), ModificaProfilo.class));
                    return true;
                case R.id.log_out://per il logout
                    FirebaseAuth.getInstance().signOut();
                    GoogleSignIn.getClient(
                            getContext(),
                            new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                    ).signOut();
                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    startActivity(intent);
                    return true;
                default:
                    return false;
            }
        });
        //Si prendono i riferimenti agli ementi grafici e, nel caso della foto e del nome vengono valorizzati.
        user = FirebaseAuth.getInstance().getCurrentUser();
        TextView textView = root.findViewById(R.id.nome);
        textView.setText(user.getDisplayName());
        ImageView photo_user = root.findViewById(R.id.ProfilePhoto);
        textViewStatus = root.findViewById(R.id.status);
        contattiRicevuti = root.findViewById(R.id.contact_list);
        Glide
                .with(getContext())
                .load(user.getPhotoUrl())
                .apply(RequestOptions.circleCropTransform())
                .into(photo_user);

        //Si leggono le informazioni dal database
        db.collection("Utenti").document(user.getUid()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            utente = (Utente) document.toObject(Utente.class);
                            setStatus();
                            setEventGenerated();

                        }
                    }
                }).addOnCompleteListener(fine -> db.collection("Utenti").document(user.getUid()).collection("Contatti").whereEqualTo("da_tracciare", true).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                //Vengono letti i contatti da tracciare
                QuerySnapshot documentSnapshots = task.getResult();
                for (DocumentSnapshot documentSnapshot : documentSnapshots.getDocuments()) {
                    ArrayList<Contatto> contacts = new ArrayList<>();
                    contacts.add(documentSnapshot.toObject(Contatto.class));
                    utente.setContatti(contacts);
                    setContattiList();
                }
            }
        }));

        return root;
    }

    //In base allo status viene visualizzato il testo dello status corrispondente
    private void setStatus() {
        String status;
        switch (utente.getStatus()) {
            case 1:
                status = getString(R.string.covid_negative);
                textViewStatus.setText(status);
                break;
            case 2:
                status = getString(R.string.covid_low_risk);
                textViewStatus.setText(status);
                break;
            case 3:
                status = getString(R.string.covid_high_risk);
                textViewStatus.setText(status);
                break;
            case 4:
                status = getString(R.string.covid_positive);
                textViewStatus.setText(status);
                break;
            case 5:
                status = getString(R.string.covid_vaccinated);
                textViewStatus.setText(status);
                break;


        }


    }

    /*
     * Viene "popoplata" la listview degli eventi di cui l'utente è admin. Per dare coerenza grafica è stato
     * creato un adapter chiamato EventCardAdapter. In caso non ci sono eventi gestiti viene visualizzata
     * una stringa. Alla listview viene aggiunto un listener per permettere, alla pressione dell'evento,
     * di avviare l'activity dell'evento relativo.
     * */
    private void setEventGenerated() {
        final ArrayList<Evento> listaEventi = new ArrayList<>();
        final EventCardAdapter adapterEventicreati = new EventCardAdapter(getActivity().getApplicationContext(), android.R.layout.simple_list_item_1, listaEventi);;
        final ArrayList<String> listaEventiID = new ArrayList<>();
        ArrayList<String> infoEventi = new ArrayList<String>();
        if (utente.getEventiGestiti().isEmpty() || utente == null) {
            ArrayAdapter<String> ad = new ArrayAdapter<String>(getActivity().getApplicationContext(), android.R.layout.simple_list_item_1, infoEventi);
            infoEventi.add(getString(R.string.manage_event_msg));
            eventiGestiti.setAdapter(ad);
        } else {
            for (String eventoID : utente.getEventiGestiti()) {
                db.collection("Eventi").document(eventoID).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Evento evento = task.getResult().toObject(Evento.class);
                        listaEventiID.add(task.getResult().getId());
                        adapterEventicreati.add(evento);
                        adapterEventicreati.notifyDataSetChanged();
                    }
                });
            }
            eventiGestiti.setAdapter(adapterEventicreati);
            eventiGestiti.setOnItemClickListener((parent, view, position, id) -> {
                    Intent intent = new Intent(getActivity(), EventActivity.class);
                    intent.putExtra("ID_Evento", listaEventiID.get(position));
                    startActivity(intent);
            });
        }
    }

    /*
     * Una lista di solo stringhe per permettere all'utente di visualizzare i suoi contatti. Per la privacy è mostrato
     * solo l'id della persona con cui ha avuto contatti l'utente e la data-
     * */
    private void setContattiList() {
        final ArrayAdapter<String> adapter;
        ArrayList<String> listaContatti = new ArrayList<String>();
        if (utente.getContatti().isEmpty() || utente == null) {
            String messaggioVuoto = getString(R.string.contact_msg);
            listaContatti.add(messaggioVuoto);
        } else {
            for (Contatto contatto : utente.getContatti()) {
                listaContatti.add("ID: " + contatto.getID_contatto() + "\n" + (getString(R.string.discover_on_day)) + ": " + getDataToPrint(contatto.getData_contatto()));
            }
        }
        backdropContainer.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        adapter = new ArrayAdapter<String>(getActivity().getApplicationContext(), android.R.layout.simple_list_item_1, listaContatti);
        contattiRicevuti.setAdapter(adapter);
    }

    private String getDataToPrint(Date date) {
        String giorno = String.format("%02d",date.getDate() );
        String mese = String.format("%02d",(date.getMonth() + 1) );
        String anno = String.format("%04d",(date.getYear()+1900) );
        return (giorno + "/" + mese + "/" + anno);
    }


}