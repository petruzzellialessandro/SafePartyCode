package com.gingerbread.SafeParty;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.gingerbread.contagiapp.R;
import com.gingerbread.SafeParty.bluetooth.BackgroundBluetoothService;
import com.gingerbread.SafeParty.dataClasses.Contatto;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;

public class HomeActivity extends AppCompatActivity {


    private FirebaseUser user;
    public static final int REQUEST_ACCESS_COARSE_LOCATION = 190;
    public static final int REQUEST_VISIBILITY = 179;
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    public void onBackPressed() {
    }

    /*
    * Questo metodo esamina la risposta al permesso di attivazione della visibilità bluetooth.
    * In caso di esito di resultCode diverso da "120", e quindi un permesso di visibilità negato,
    * viene effettuato il logout, con un toast che segnala la mancata possibilità di utilizzare
    * l'app senza bluetooth. Infine, Firebase effettua la disconnessione dell'utente, riportandolo
    * all'activity principale.
    * */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_VISIBILITY:
                if (resultCode != 120) {
                    Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_LONG).show();
                    FirebaseAuth.getInstance().signOut();
                    super.onBackPressed();
                }
                else
                {
                    checkCoarseLocationPermission();
                }
                break;
            default:
                Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_LONG).show();
                FirebaseAuth.getInstance().signOut();
                finish();
                super.onBackPressed();
                break;
        }
    }

    /*
    * Questo metodo inizializza l'interfaccia grafica del fragment home.
    * Imposta la barra di navigazione inferiore (navbar).
    * */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        user = FirebaseAuth.getInstance().getCurrentUser();
        setContentView(R.layout.activity_home);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupWithNavController(navView, navController);

    }

    /*
     * In questo metodo salviamo in una memoria condivisa l'ID dell'utente il cui è utile per il
     * tracciamento e verifichiamo che sul dispositivo sia disponibile il bluetooth. Se l'ID
     * dell'utente non è presente allora attraverso Firebase viene eseguito il logout.
     * Se il dispositivo non è fornito di bluetooth allora apparirà un Dialog che avvisa che non
     * sarà possibile utilizzare l'app e viene effettuato il logout, altrimenti, se il bluetooth è
     * disponibile, viene controllato se è attivo. Se non è attivo viene richiesta l'attivazione
     * e viene invocato il metodo checkCoarseLocationPermission().
     */
    @Override
    protected void onStart() {
        super.onStart();
        saveRemoteContact();
        SharedPreferences preferences = getApplicationContext().getSharedPreferences("com.gingerbread.SafeParty", Context.MODE_PRIVATE);
        String UID_Utente = preferences.getString("UID_Utente", "Not_defined");
        if (UID_Utente.equals("Not_defined")) {
            FirebaseAuth.getInstance().signOut();
            return;
        }
        if (bluetoothAdapter == null) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.bt_unavailable_title))
                    .setMessage(getString(R.string.bluetooth_unavailable))
                    .setPositiveButton("Ok", (dialog, which) -> {
                        FirebaseAuth.getInstance().signOut();
                        finish();
                        System.exit(0);
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBt = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                startActivityForResult(enableBt, REQUEST_VISIBILITY);
            }
            else
            {
                checkCoarseLocationPermission();
            }
        }
    }

    /*
    * Questo metodo crea e fa runnare un thread per non bloccare l'esecuzione del'app. Questo thread
    * effettua le operazioni ogni ora. Se esiste il file "ContactDat.dat" (file in cui vengono salvati
    * i contatti con altre persone) da questo vengono letti tutti i contatti. Per ogni contatto viene
    * verificato se è già presente. Se presente vengono aggioranti la data del contatto e il flag
    * "da_tracciare". Se il contatto non è presente allora lo si salva nel database.
    * L'operazione di salvataggio avviene sugli account di entrambi gli utenti. Cosi se lo smartphone
    * di A registra un contatto di B anche nella sezione contatti di B sarà presente il contatto con A.
    * Ultimo "lavoro" svolto nel thread è il controllo, per ogni contatto dell'untente salvato sul DB,
    * della data. Se la data è antecedente di almeno 14 quella in cui viene effettuato il controllo
    * allora il flag "da_tracciare è impostato a false"
    * */
    private void saveRemoteContact() {
        new Thread(() -> {
            while (true) {
                File dir = new File(getApplicationContext().getFilesDir(), "/SafeParty");
                if (dir.exists()) {
                    File file = new File(dir, "ContactData.dat");
                    try {
                        if (file.exists()) {
                            FileReader reader = new FileReader(file);
                            BufferedReader bufferedReader = new BufferedReader(reader);
                            String line;
                            while ((line = bufferedReader.readLine()) != null) {
                                String IDcontatto = line.split(",")[0];
                                String DataTesto = line.split(",")[1];
                                DataTesto = DataTesto.split(";")[0];
                                Date data = new Date(DataTesto);
                                db.collection("Utenti").document(FirebaseAuth.getInstance().getUid()).collection("Contatti").whereEqualTo("id_contatto", IDcontatto).get().addOnCompleteListener(
                                        task -> {
                                            if (task.getResult() != null) {
                                                if (task.getResult().getDocuments().isEmpty()) {
                                                    db.collection("Utenti").document(FirebaseAuth.getInstance().getUid()).collection("Contatti").add(new Contatto(IDcontatto, data, true));
                                                } else {
                                                    for (DocumentSnapshot documentSnapshot : task.getResult().getDocuments()) {
                                                        Contatto contattoLetto = documentSnapshot.toObject(Contatto.class);
                                                        if (contattoLetto.getData_contatto().before(data)) {
                                                            db.collection("Utenti").document(FirebaseAuth.getInstance().getUid()).collection("Contatti").document(documentSnapshot.getId()).set(new Contatto(IDcontatto, data, true));

                                                        }
                                                    }
                                                }
                                            }
                                        }
                                );
                                db.collection("Utenti").document(IDcontatto).collection("Contatti").whereEqualTo("id_contatto", FirebaseAuth.getInstance().getUid()).get().addOnCompleteListener(
                                        task -> {
                                            if (task.getResult() != null) {
                                                if (task.getResult().getDocuments().isEmpty()) {
                                                    db.collection("Utenti").document(IDcontatto).collection("Contatti").add(new Contatto(FirebaseAuth.getInstance().getUid(), data, true));
                                                } else {
                                                    for (DocumentSnapshot documentSnapshot : task.getResult().getDocuments()) {
                                                        Contatto contattoLetto = documentSnapshot.toObject(Contatto.class);
                                                        if (contattoLetto.getData_contatto().before(data)) {
                                                            db.collection("Utenti").document(IDcontatto).collection("Contatti").document(documentSnapshot.getId()).set(new Contatto(FirebaseAuth.getInstance().getUid(), data, true));
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                );
                            }
                        }
                    } catch (IOException e) {
                    }
                }
                db.collection("Utenti").document(FirebaseAuth.getInstance().getUid()).collection("Contatti").whereEqualTo("da_tracciare", true).get().addOnCompleteListener(
                        task -> {
                            if (task.getResult() != null) {
                                if (!task.getResult().getDocuments().isEmpty()) {
                                    for (DocumentSnapshot documentSnapshot : task.getResult().getDocuments()) {
                                        Contatto contattoLetto = documentSnapshot.toObject(Contatto.class);
                                        long difference = (new Date()).getTime() - contattoLetto.getData_contatto().getTime();
                                        long day = (((difference / 1000) / 3600) / 24);
                                        if (day >= 14) {
                                            contattoLetto.setDa_tracciare(false);
                                            db.collection("Utenti").document(FirebaseAuth.getInstance().getUid()).collection("Contatti").document(documentSnapshot.getId()).set(contattoLetto);
                                        }
                                    }
                                }
                            }
                        }
                );
                try {
                    Thread.sleep(3600000); //1h in millis
                } catch (InterruptedException e) {

                    e.printStackTrace();
                }
            }
        }).start();
    }
    /*
     * In questo metodo andiamo a chiedere all'utente i permessi per tracciare la sua posizione,
     * leggere e scrivere su file. Il metodo infatti controlla inizialmente la versione di android dell'utente.
     * In base al risultato decide se richiedere, insieme ai permessi di letture e scrittura su file, "ACCESS_COARSE_LOCATION"
     * oppure "ACCESS_FINE_LOCATION" se la versione di android è più reccente.
     * */
    private void checkCoarseLocationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) ||
                    (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) ||
                    (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_ACCESS_COARSE_LOCATION);
            } else {
                Intent backgroundService = new Intent(getApplicationContext(), BackgroundBluetoothService.class);
                BackgroundBluetoothService.enqueueWork(this, backgroundService);
                Intent backgroundServiceNotification = new Intent(getApplicationContext(), NotificationService.class);
                NotificationService.enqueueWork(this, backgroundServiceNotification);

            }
        } else {
            if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) ||
                    (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) ||
                    (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_ACCESS_COARSE_LOCATION);
            } else {
                Intent backgroundService = new Intent(getApplicationContext(), BackgroundBluetoothService.class);
                BackgroundBluetoothService.enqueueWork(this, backgroundService);
                Intent backgroundServiceNotification = new Intent(getApplicationContext(), NotificationService.class);
                NotificationService.enqueueWork(this, backgroundServiceNotification);
            }
        }
    }

    /*
     * Metodo invocato come risultato di checkCoarseLocationPermission(). Se l'utente ha dato l'ok a
     * tutte le richieste dell'app, si avvierà il BluetoothService e verrà visualizzato un toast che ne attesta il successo.
     * Se l'utente invece non da l'assenso a tutti i permessi richiesti, visualizzerà la schermata
     * d'accesso accompagnato da un toast che spiega l'impossibilità, per l'app, di funzinare senza i permessi richiesti.
     * */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_ACCESS_COARSE_LOCATION:
                if (grantResults.length > 0) {
                    for (int permesso : grantResults) {
                        if (permesso != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_LONG).show();
                            FirebaseAuth.getInstance().signOut();
                            finish();
                            super.onBackPressed();
                            return;
                        }
                    }
                    Toast.makeText(this, getString(R.string.permission_allowed), Toast.LENGTH_LONG).show();
                    Intent backgroundService = new Intent(getApplicationContext(), BackgroundBluetoothService.class);
                    BackgroundBluetoothService.enqueueWork(this, backgroundService);
                    Intent backgroundServiceNotification = new Intent(getApplicationContext(), NotificationService.class);
                    NotificationService.enqueueWork(this, backgroundServiceNotification);
                    break;
                }
        }
    }
}