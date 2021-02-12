package com.gingerbread.SafeParty.ui.event;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.gingerbread.contagiapp.R;
import com.gingerbread.SafeParty.dataClasses.Evento;
import com.gingerbread.SafeParty.dataClasses.Utente;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

public class EventCreation extends AppCompatActivity {

    private static final String api = "AIzaSyDqOhJBYPBYovZdAAGWQFXbpyydaK6qnzY";
    private TextInputEditText nomeEvento;
    private TextInputEditText numeroPartecipanti;
    private TextInputEditText nomeDescrizione;
    private SwitchMaterial Switchprivato;
    private TextInputEditText dataEvento;
    private Date data;
    private String via;
    private String città;
    private String regione;
    private String nazione;
    private double lat = 0;
    private double lon = 0;
    private boolean privato;
    Spinner status;
    ArrayAdapter<String> adpter;
    Utente utenteLetto;
    DatePickerDialog.OnDateSetListener mDateListener;
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_creation);
        nomeEvento =  findViewById(R.id.edit_name);
        numeroPartecipanti =  findViewById(R.id.participantNumber);
        nomeDescrizione =  findViewById(R.id.eventDescription);
        Switchprivato = (SwitchMaterial)findViewById(R.id.private_event);
        dataEvento = findViewById(R.id.event_date);
        status = findViewById(R.id.spinnerStatus);


        if (savedInstanceState != null)
        {
            String nome = savedInstanceState.getString("Nome");
            String cognome = savedInstanceState.getString("Partecipanti");
            String descrizione = savedInstanceState.getString("Descrizione");
            String dataEventoS = savedInstanceState.getString("Data Evento");
            this.privato = savedInstanceState.getBoolean("Privato");
            Switchprivato.setChecked(privato);
            nomeEvento.setText(nome);
            numeroPartecipanti.setText(cognome);
            nomeDescrizione.setText(descrizione);
            dataEvento.setText(dataEventoS);
        }
        adpter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new String[]{getString(R.string.covid_negative), getString(R.string.covid_low_risk), getString(R.string.covid_high_risk), getString(R.string.covid_positive), getString(R.string.covid_vaccinated)});
        status.setAdapter(adpter);
        Switchprivato.setOnCheckedChangeListener((buttonView, isChecked) -> privato = isChecked);
        dataEvento.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH);
            int day = cal.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dialog =  new DatePickerDialog(
                    this,
                    AlertDialog.THEME_DEVICE_DEFAULT_DARK,
                    mDateListener,
                    year,month,day
            );
            dialog.show();
        });
        mDateListener = (datePicker, year, month, day) -> {

            dataEvento.setText(day+ "/"+ month+ "/"+ year);
            Calendar mcurrentTime = Calendar.getInstance();
            int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
            int minute = mcurrentTime.get(Calendar.MINUTE);
            TimePickerDialog mTimePicker;
            mTimePicker = new TimePickerDialog(this, (timePicker, selectedHour, selectedMinute) -> {
                data = new Date(year - 1900, month, day, selectedHour, selectedMinute);
                dataEvento.setText(day+ "/"+ (month+1)+ "/"+ year + " " + selectedHour + ":"+ selectedMinute);
            }, hour, minute, true);
            mTimePicker.show();
        };


        Button cancel = findViewById(R.id.cancelEventInfo);
        cancel.setOnClickListener(v -> this.tornaIndietro());

        Places.initialize(getApplicationContext(), api);
        PlacesClient placesClient = Places.createClient(this);
        AutocompleteSupportFragment autocompleteSupportFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        autocompleteSupportFragment.setTypeFilter(TypeFilter.ADDRESS);
        autocompleteSupportFragment.setLocationBias(RectangularBounds.newInstance(
                new LatLng(-33.880490, 151.184363),
                new LatLng(-33.858754, 151.229596)));
        autocompleteSupportFragment.setPlaceFields(Arrays.asList(Place.Field.LAT_LNG,Place.Field.ADDRESS_COMPONENTS, Place.Field.NAME));
        autocompleteSupportFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {

                    via = place.getName();
                    lat =  place.getLatLng().latitude;
                    lon = place.getLatLng().longitude;
                    try
                    {
                        Integer.parseInt(place.getAddressComponents().asList().get(0).getName());
                        città = place.getAddressComponents().asList().get(2).getName();
                        regione = place.getAddressComponents().asList().get(5).getName();
                        nazione = place.getAddressComponents().asList().get(6).getName();
                    }catch (NumberFormatException e)
                    {
                        città = place.getAddressComponents().asList().get(1).getName();
                        regione = place.getAddressComponents().asList().get(4).getName();
                        nazione = place.getAddressComponents().asList().get(5).getName();
                    }
            }

            @Override
            public void onError(@NonNull Status status) {
                Log.d("LUOGO", "Errore motrale "+status );
            }
        });
    }
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("Nome", nomeEvento.getText().toString());
        outState.putString("Partecipanti", numeroPartecipanti.getText().toString());
        outState.putString("Descrizione", nomeDescrizione.getText().toString());
        outState.putString("Data Evento", dataEvento.getText().toString());
        outState.putBoolean("Privato", privato);
    }

    private void tornaIndietro(){
        new AlertDialog.Builder(EventCreation.this)
                .setTitle(getString(R.string.event_go_back_alert_title))
                .setMessage(getString(R.string.event_go_back_alert_text))
                .setPositiveButton(getString(R.string.go_back_alert), (dialog, which) -> {
                    super.onBackPressed();
                })
                .setNegativeButton(getString(R.string.stay_here_alert), (dialog,which) -> {
                    dialog.dismiss();
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    @Override
    public void onBackPressed() {
        this.tornaIndietro();
    }
    public void creaEvento(View w){
        int numPartecipanti;
        try {
            numPartecipanti = Integer.parseInt(numeroPartecipanti.getText().toString());
        }catch (NumberFormatException e){
            numPartecipanti = -1;
        }
        if (
                (nomeEvento.getText().length() < 5) ||
                (nomeDescrizione.getText().length()<10)||
                (nomeDescrizione.getText().length()<10)||
                (data.before(new Date())) ||
                (lat == 0) ||
                (lon == 0) ||
                (numPartecipanti < 2)
        )
        {
            new AlertDialog.Builder(EventCreation.this)
                    .setTitle(getString(R.string.check_fields_title))
                    .setMessage(getString(R.string.check_event_form))
                    .setPositiveButton(getString(R.string.understood_alert), (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
        else
        {
            ArrayList<String> gestori = new ArrayList<String>();
            gestori.add(FirebaseAuth.getInstance().getUid());
            Evento eventoCreato = new Evento(nomeEvento.getText().toString(), nomeDescrizione.getText().toString(), numPartecipanti, privato, data, via, città, regione, nazione, lat, lon, gestori, new ArrayList<String>(), new ArrayList<String>(), adpter.getPosition(status.getSelectedItem().toString())+1);
            db.collection("Eventi").add(eventoCreato).addOnCompleteListener(task -> {
                if (task.isSuccessful())
                {
                    db.collection("Utenti").document(FirebaseAuth.getInstance().getUid()).get().addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()){
                            utenteLetto = task1.getResult().toObject(Utente.class);
                            utenteLetto.addEventiGestiti(task.getResult().getId());
                            db.collection("Utenti").document(FirebaseAuth.getInstance().getUid()).update("eventiGestiti", utenteLetto.getEventiGestiti()).addOnCompleteListener(task2 -> {
                                Intent intent = new Intent(getApplicationContext(), EventActivity.class);
                                intent.putExtra("ID_Evento", task.getResult().getId());
                                startActivity(intent);
                            });
                        }
                    });
                }
            });
        }
    }
}