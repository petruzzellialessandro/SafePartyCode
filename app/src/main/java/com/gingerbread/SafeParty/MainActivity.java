package com.gingerbread.SafeParty;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.gingerbread.contagiapp.R;
import com.gingerbread.SafeParty.dataClasses.Utente;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
/*
*   Entry point dell'app. In questa activity viene permesso all'untente di effettuare il login (o registrarsi)
*   con Google. L'implementazione del Login avviene per mezzo di Firebase auth. I metodi sono stati realizzati
*   avendo come base il codice messo a disposizione nella documentazione ufficiale di firebase
* */
public class MainActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 3000;
    private FirebaseAuth firebaseAuth;
    private GoogleSignInOptions googleSignInOptions;
    private GoogleSignInClient googleSignInClient;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /*
    * googleSignInOption è stato "costruito" seguendo la documentazione presente su firebase.
    * Se l'utente è già loggato allora viene direttamente invocato il metodo startHomeActivity()
    * */
    @Override
    protected void onStart() {
        super.onStart();
        googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);
        firebaseAuth = FirebaseAuth.getInstance();
        startHomeActivity(firebaseAuth.getCurrentUser());
    }

    public void singInWithGoogle(View v) {
        Intent singInIntent = googleSignInClient.getSignInIntent();
        startActivityIfNeeded(singInIntent, RC_SIGN_IN);
    }

    /*
    * Metodo invocato come risultato di getSignInIntent(). L'implementazione è presa dalla documentazione ufficiale
    * di Firebase. Se l'operazione fallisce allora, molto probabilmente, l'utente non ha la connessione.
    * Ad ogni modo viene visualizzato un toast con un messaggio di errore
    * */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Toast.makeText(this, getString(R.string.error_msg), Toast.LENGTH_LONG).show();
            }
        }
    }

    /*
     *  In questo metodo salviamo l'utente non solo su FirebaseAuth ma anche sul Database Firestore.
     *  Se esiste già esiste viene effettuato un aggiornamento del nome e del link della foto profilo.
     *  Questa scelta è stata effettuata per tenere la foto profilo sempre aggiornata.
     * */
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        Utente utente = new Utente(1, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), acct.getEmail(), user.getPhotoUrl().toString(), user.getDisplayName());
                        db.collection("Utenti").document(user.getUid()).get().addOnCompleteListener(task1 -> {
                            if (!task1.getResult().exists()) {
                                db.collection("Utenti").document(user.getUid()).set(utente);
                            }
                            else
                            {
                                db.collection("Utenti").document(user.getUid()).update("photoURL", user.getPhotoUrl().toString(), "name",user.getDisplayName());
                            }
                        });
                        startHomeActivity(user);
                    } else {
                        Toast.makeText(getBaseContext(), getString(R.string.error_msg), Toast.LENGTH_LONG).show();
                    }
                });
    }
/*
* Si avvia l'activity della Home passando come extra l'ID delle utente che ha effettuato l'accesso.
* L'ID è anche salvato nelle sharedPreferences. Sarà l'informazione che identifica l'utente nel tracciamento bluetooh
* */
    private void startHomeActivity(FirebaseUser user) {
        if (user != null) {
            SharedPreferences preferences = getApplicationContext().getSharedPreferences("com.gingerbread.SafeParty", Context.MODE_PRIVATE);
            String UID_Utente = preferences.getString("UID_Utente", "Not_defined");
            if (UID_Utente.equals("Not_defined")) {
                preferences.edit().putString("UID_Utente", user.getUid()).commit();
            }
            Intent homeActivity = new Intent(this, HomeActivity.class);
            startActivity(homeActivity);
        }
    }


}