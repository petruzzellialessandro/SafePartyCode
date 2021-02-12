package com.gingerbread.SafeParty.ui.event;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.gingerbread.SafeParty.HomeActivity;
import com.gingerbread.SafeParty.ui.event.adapter.EventUserAdapter;
import com.gingerbread.contagiapp.R;
import com.gingerbread.SafeParty.dataClasses.Evento;
import com.gingerbread.SafeParty.dataClasses.Utente;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.dynamiclinks.DynamicLink;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;

public class EventActivity extends AppCompatActivity {
    String Id_Evento = null;
    Evento evento;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    androidx.appcompat.widget.Toolbar toolbar;
    private TextView dateAndTime;
    private TextView cardDescription;
    private ImageView mapImage;
    private ProgressBar progressBar;
    private ScrollView scrollView;
    private EventUserAdapter eventUserAdapter;
    private EventUserAdapter eventUserAdapterAdmin;
    private TextView cardPlace;
    private TextView numberParticipants;
    private TextView requiredStatus;
    private Button joinEvent;
    private ListView partecipantList;
    private ListView adminList;
    private TextInputLayout newAdmin;
    private TextInputEditText emailAdmin;
    private com.google.android.material.button.MaterialButton addAdmin;
    ArrayList<Utente> partecipanti;
    ArrayList<Utente> gestori;
    private final int Vaccinato = 5;
    private final int Positivo = 4;
    private final int AltoRischio = 3;
    private final int BassoRischio = 2;
    private final int Negativo = 1;

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState, @NonNull PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        outState.putString("emailAdmin", emailAdmin.getText().toString());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);
        Intent i = getIntent();
        toolbar = findViewById(R.id.toolbarEvento);
        dateAndTime = findViewById(R.id.datetime_card_evento);
        cardDescription = findViewById(R.id.description_card_evento);
        mapImage = findViewById(R.id.mapImageView);
        cardPlace = findViewById(R.id.card_place);
        progressBar = findViewById(R.id.loadingEventInfo);
        scrollView = findViewById(R.id.scrollCart);
        scrollView.setVisibility(View.GONE);
        numberParticipants = findViewById(R.id.n_participant_card_evento);
        requiredStatus = findViewById(R.id.status_card_evento);
        joinEvent = findViewById(R.id.joinEvent);
        partecipantList = findViewById(R.id.participant_list);
        adminList = findViewById(R.id.admin_list);
        newAdmin = findViewById(R.id.newAdmin);
        emailAdmin = findViewById(R.id.email_new_admin);
        addAdmin = findViewById(R.id.addAdmin);
        if (savedInstanceState!=null){
            String email = savedInstanceState.getString("emailAdmin");
            emailAdmin.setText(email);
        }
        toolbar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {

                case R.id.action_share:
                    Intent intentShare = new Intent();
                    intentShare.setAction(Intent.ACTION_SEND);
                    try {
                        String message = getString(R.string.share_event_msg) + "\n" + URLDecoder.decode(getDynmicLink().toString(), "UTF-8");
                        intentShare.putExtra(Intent.EXTRA_TEXT, message);
                        intentShare.setType("text/plain");
                        startActivity(Intent.createChooser(intentShare, null));
                    } catch (UnsupportedEncodingException e) {
                        Toast.makeText(this, getString(R.string.general_connection_error), Toast.LENGTH_LONG).show();
                    }
                    return true;
                case R.id.action_get_request:
                    Intent reuest = new Intent(this, EventRequest.class);
                    reuest.putExtra("ID_Evento", Id_Evento);
                    startActivity(reuest);
                default:
                    return false;
            }
        });
        if (i.getExtras().getString("ID_Evento") != null) {
            Id_Evento = i.getExtras().getString("ID_Evento");
        } else if (getIntent().getData() != null) {
            Id_Evento = getIntent().getData().toString().split("=")[2];
        }
        if (Id_Evento == null) {
            Toast.makeText(this, getString(R.string.loading_event_error), Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, HomeActivity.class));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        db.collection("Eventi").document(Id_Evento).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                evento = task.getResult().toObject(Evento.class);
                setToolbar();
                setEventContent();
                setPartecipantList();
                setAdminList();
                progressBar.setVisibility(View.GONE);
                scrollView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setAdminList() {
        gestori = new ArrayList<Utente>();
        eventUserAdapterAdmin = new EventUserAdapter(getBaseContext(),  android.R.layout.simple_list_item_1,gestori);
        adminList.setAdapter(eventUserAdapterAdmin);
        for (String idPartecipanti : evento.getGestori()){
            db.collection("Utenti").document(idPartecipanti).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()){
                    Utente utente = task.getResult().toObject(Utente.class);
                    eventUserAdapterAdmin.add(utente);
                    eventUserAdapterAdmin.notifyDataSetChanged();
                    setListViewHeightBasedOnItems(adminList);
                }
            });
        }
    }

    private void setPartecipantList() {
        partecipanti = new ArrayList<Utente>();
        eventUserAdapter = new EventUserAdapter(getBaseContext(),  android.R.layout.simple_list_item_1,partecipanti);
        partecipantList.setAdapter(eventUserAdapter);
            for (String idPartecipanti : evento.getPartecipanti()){
                db.collection("Utenti").document(idPartecipanti).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()){
                        Utente utente = task.getResult().toObject(Utente.class);
                        eventUserAdapter.add(utente);
                        eventUserAdapter.notifyDataSetChanged();

                        setListViewHeightBasedOnItems(partecipantList);
                    }
                });
            }
    }

    private void setEventContent() {
        cardDescription.setText(evento.getDescrizione());
        String ora = String.format("%02d",evento.getData().getHours() );
        String minuti = String.format("%02d",evento.getData().getMinutes() );
        dateAndTime.setText(getDataToPrint(evento.getData()) + " - " + ora + ":" + minuti);

        RequestOptions corners = new RequestOptions();
        corners = corners.transform(new RoundedCorners(36));

        Glide
                .with(this)
                .load("https://maps.google.com/maps/api/staticmap?center=" + evento.getLat() + "," + evento.getLon() + "&zoom=15&size=600x350&markers=size:big&markers=color:red|" + evento.getLat() + "," + evento.getLon() + "|&sensor=false&key=AIzaSyDqOhJBYPBYovZdAAGWQFXbpyydaK6qnzY") // the uri you got from Firebase
                .apply(corners)
                .into(mapImage); //Your imageView variable

        cardPlace.setText(evento.getVia() + "\n" + evento.getCittà() + ", " + evento.getRegione() + ", " + evento.getNazione());
        numberParticipants.setText(String.valueOf(evento.getNumPartecipanti()));
        int reqStatus = evento.getStatusRichiesto();
        switch (reqStatus) {
            case Negativo:
                requiredStatus.setText(R.string.covid_negative);
                break;
            case BassoRischio:
                requiredStatus.setText(R.string.covid_low_risk);
                break;
            case AltoRischio:
                requiredStatus.setText(R.string.covid_high_risk);
                break;
            case Positivo:
                requiredStatus.setText(R.string.covid_positive);
                break;
            case Vaccinato:
                requiredStatus.setText(R.string.covid_vaccinated);
        }


        mapImage.setOnClickListener(v -> {
            mapImage.startAnimation(AnimationUtils.loadAnimation(this, R.anim.amin_item));
            try {
                Uri intentUri = Uri.parse("geo:0,0?q=" + evento.getLat() + "," + evento.getLon() + "(Place)");
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, intentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
            } catch (ActivityNotFoundException e) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com/maps/place/" + evento.getLat() + "," + evento.getLon()));
                startActivity(browserIntent);
            }

        });
    }

    private String getDataToPrint(Date date) {
        String giorno = String.format("%02d",date.getDate() );
        String mese = String.format("%02d",(date.getMonth() + 1) );
        String anno = String.format("%04d",(date.getYear()+1900) );
        return (giorno + "/" + mese + "/" + anno);
    }

    private void setToolbar() {
        toolbar.getMenu().clear();
        toolbar.setTitle(evento.getNome());
        if (evento.getGestori().contains(FirebaseAuth.getInstance().getUid())) {
            toolbar.inflateMenu(R.menu.menu_evento_gestore);
            joinEvent.setVisibility(View.GONE);
            newAdmin.setVisibility(View.VISIBLE);
            addAdmin.setVisibility(View.VISIBLE);
        } else {
            toolbar.inflateMenu(R.menu.menu_evento_partecipante);
            if (evento.getPartecipanti().contains(FirebaseAuth.getInstance().getUid())){
                joinEvent.setVisibility(View.GONE);
            }
        }
    }

    private Uri getDynmicLink() {
        Uri dynamicLinkUri = null;
        if (Id_Evento != null) {
            DynamicLink dynamicLink = FirebaseDynamicLinks.getInstance().createDynamicLink()
                    .setLink(Uri.parse(Id_Evento))
                    .setDomainUriPrefix("https://contagiapp.page.link")
                    .setAndroidParameters(new DynamicLink.AndroidParameters.Builder("com.gingerbread.contagiapp").build())
                    .buildDynamicLink();

            dynamicLinkUri = dynamicLink.getUri();
            try {
                Log.d("LINKDIFIREBASE", URLDecoder.decode(dynamicLinkUri.toString(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return dynamicLinkUri;
    }


    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, HomeActivity.class));
    }

    public void tornaIndietro(View v) {
        new AlertDialog.Builder(EventActivity.this)
                .setTitle(getString(R.string.event_go_back_alert_title))
                .setMessage(getString(R.string.event_go_back_alert_text))
                .setPositiveButton(getString(R.string.go_back_alert), (dialog, which) -> {
                    super.onBackPressed();
                })
                .setNegativeButton(getString(R.string.stay_here_alert), (dialog, which) -> {
                    dialog.dismiss();
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }


    /*
    * Questo metodo è stato in parte presto da StackOverFlow. L'obiettivo è quello di adattare
    * l'altezza della cart al contenuto (wrap_content non funziona se il contenuto è una listView)
    * E' stato in parte migliorato per permettere una visualizzazione coerente con lo stile grafico della nostra app.
    * */
    public static boolean setListViewHeightBasedOnItems(ListView listView) {

        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter != null) {

            int numberOfItems = listAdapter.getCount();

            int totalItemsHeight = 100;
            for (int itemPos = 0; itemPos < numberOfItems; itemPos++) {
                View item = listAdapter.getView(itemPos, null, listView);
                float px = 500 * (listView.getResources().getDisplayMetrics().density);
                item.measure(View.MeasureSpec.makeMeasureSpec((int)px, View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                totalItemsHeight += item.getMeasuredHeight();
            }

            int totalDividersHeight = listView.getDividerHeight() *
                    (numberOfItems - 1);
            int totalPadding = listView.getPaddingTop() + listView.getPaddingBottom();

            ViewGroup.LayoutParams params = listView.getLayoutParams();
            params.height = totalItemsHeight + totalDividersHeight + totalPadding;
            listView.setLayoutParams(params);
            listView.requestLayout();
            return true;

        } else {
            return false;
        }

    }
    public void partecipatioRequest(View v){
        if (evento.getPartecipanti().size() == evento.getNumPartecipanti()){
            Toast.makeText(this, getString(R.string.event_full_toast), Toast.LENGTH_LONG).show();
            return;
        }
        if (evento.getRichieste().contains(FirebaseAuth.getInstance().getUid())){
            Toast.makeText(this, getString(R.string.request_already_sent), Toast.LENGTH_LONG).show();
            return;
        }
        if (evento.getPartecipanti().contains(FirebaseAuth.getInstance().getUid())){
            Toast.makeText(this, getString(R.string.already_participant), Toast.LENGTH_LONG).show();
            return;
        }
        db.collection("Utenti").document(FirebaseAuth.getInstance().getUid()).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                Utente utente = task.getResult().toObject(Utente.class);
                if ((utente.getStatus() ==  Vaccinato) || ( utente.getStatus()<=evento.getStatusRichiesto())){
                    evento.addRichiesta(FirebaseAuth.getInstance().getUid());
                    utente.addEventiRichiesti(Id_Evento);
                    new Thread(()->{
                        db.collection("Utenti").document(FirebaseAuth.getInstance().getUid()).set(utente).addOnCompleteListener(task1 -> db.collection("Eventi").document(Id_Evento).set(evento)).addOnCompleteListener(task12 -> {
                            Toast.makeText(getApplicationContext(), getString(R.string.sending_request_toast), Toast.LENGTH_LONG).show();
                            onBackPressed();
                        });
                    }).start();
                }
                else
                {
                    Toast.makeText(this, getString(R.string.rejected_status_toast), Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    public void addAdmin(View v){
        String email = emailAdmin.getText().toString();
        db.collection("Utenti").whereEqualTo("email", email).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                if (task.getResult().isEmpty()){
                    Toast.makeText(getApplicationContext(), getString(R.string.unregistered_email), Toast.LENGTH_LONG).show();
                }
                else{
                    String idUtente = task.getResult().getDocuments().get(0).getId();
                    if (evento.getGestori().contains(idUtente)){
                        Toast.makeText(getApplicationContext(), getString(R.string.already_admin), Toast.LENGTH_LONG).show();
                    }else
                    {
                        Utente utente=task.getResult().getDocuments().get(0).toObject(Utente.class);
                        utente.addEventiGestiti(Id_Evento);
                        evento.addGestore(idUtente);
                        new Thread(()->{
                            db.collection("Utenti").document(idUtente).set(utente).addOnCompleteListener(task1 -> {
                                if (task1.isSuccessful()){
                                    db.collection("Eventi").document(Id_Evento).set(evento).addOnCompleteListener(task11 -> {
                                        if (task11.isSuccessful()){
                                            Toast.makeText(getApplicationContext(), getString(R.string.admin_correctly_added), Toast.LENGTH_LONG).show();
                                            emailAdmin.setText("");
                                            InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
                                            inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                                            this.setAdminList();
                                        }
                                    });
                                }
                            });
                        }).start();
                    }
                }
            }
        });
    }

}