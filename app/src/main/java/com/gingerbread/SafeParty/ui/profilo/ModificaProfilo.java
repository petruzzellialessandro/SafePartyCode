package com.gingerbread.SafeParty.ui.profilo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.gingerbread.SafeParty.HomeActivity;
import com.gingerbread.SafeParty.dataClasses.Utente;
import com.gingerbread.contagiapp.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.HashMap;

public class ModificaProfilo extends AppCompatActivity {

    FirebaseFirestore db = FirebaseFirestore.getInstance();
    private RadioGroup radioStatusGroup;
    private FirebaseFunctions mFunctions = FirebaseFunctions.getInstance();
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private Utente utente = null;
    HashMap<String, Object> data = new HashMap<>();
    int newStatus = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modifica_profilo);

    }


    /*
     * Questo metodo si occupa di gestire il cambiamento di status dell'utente. Questo metodo è direttamente
     * associato alla pressione del tasto "Salva" ("Save" in Inglese).
     * Ultima operazione è il salvataggio sul database del nuovo status. Quando l'operazione va a buon fine l'utente
     * viene rimandato alla home accompagnato da un toast. Alla modifca dello status chiama una cloud function
     * di firebase. Il codice della funzione (Scritta in JS) si trova nella cartella "functions" nel file index.js
     * */
    public void modificaStatus(View v) {


        radioStatusGroup = (RadioGroup) findViewById(R.id.radioStatus);
        int selectedRadioIndex = radioStatusGroup.indexOfChild(findViewById(radioStatusGroup.getCheckedRadioButtonId()));


        switch (selectedRadioIndex) {

            case 0:
                newStatus = 1;
                break;
            case 1:
                newStatus = 4;
                break;
            case 2:
                newStatus = 5;
                break;

        }
        db.collection("Utenti").document(user.getUid()).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                utente = task.getResult().toObject(Utente.class);
            }
            Log.d("TASKONE", utente.getName());
            data.put("Id", user.getUid());
            data.put("OldStatus", utente.getStatus());
            data.put("NewStatus", (newStatus));
            db.collection("Utenti").document(user.getUid()).update("status", newStatus)
                    .addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()) {
                            mFunctions.getHttpsCallable("callUpdateUsersStatus").call(data);
                            Toast.makeText(this, getString(R.string.status_changes_confirm), Toast.LENGTH_LONG).show();
                            startActivity(new Intent(this, HomeActivity.class));
                            ;
                        }
                    });
        });

    }


    @Override
    public void onBackPressed() {
        this.tornaIndietro(null);
    }

    /*
     * AlterDialog per avvisare l'utente che, tornando indietro, le modiche non saranno salvate
     *  */
    public void tornaIndietro(View v) {
        new AlertDialog.Builder(ModificaProfilo.this)
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
}