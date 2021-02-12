package com.gingerbread.SafeParty;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.gingerbread.SafeParty.bluetooth.BackgroundBluetoothService;
import com.gingerbread.contagiapp.R;
import com.gingerbread.SafeParty.dataClasses.Evento;
import com.gingerbread.SafeParty.dataClasses.Utente;
import com.gingerbread.SafeParty.ui.event.EventActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class NotificationService extends JobIntentService {
    ArrayList<Evento> eventiGestiti = new ArrayList<>();
    int status;
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    public NotificationService() {
    }



    /*
     * Questo metodo serve ad inviare una notifica nel caso in cui lo status dell'utente
     * cambia e, quindi, viene modificato. L'If viene inserito nel metodo per contollare la versione
     * dell'SDK, dato che dalla versione API 26 ci sono delle differenze.
     **/
    private void sendStatusNotification(){
        Intent intent = new Intent(this, HomeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,0);
        if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.O){
            NotificationChannel notificationChannel = new NotificationChannel("SafeParty", "SafeParty", NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription("SafeParty notification");
            notificationChannel.setShowBadge(true);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "SafeParty")
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_round))
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(getApplicationContext().getResources().getString(R.string.status_notification_title))
                .setContentText(getApplicationContext().getResources().getString(R.string.status_notification_text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(2, builder.build());
    }

    /*
     * Questo metodo serve ad inviare una notifica nel caso in cui viene ricevuta una richiesta di
     * partecipazione a un evento. Come paramentro riceve l'ID dell'evento in questione.
     * Questo avviene perché la pressione sulla notifica deve riportare EventActivity dell'evento corrisponte.
     *  L'If viene inserito nel metodo per contollare la versione
     * dell'SDK, dato che dalla versione API 26 ci sono delle differenze.
     * */
    private void sendRequestNotification(String IDEvento){
        Intent intent = new Intent(this, EventActivity.class);
        intent.putExtra("ID_Evento", IDEvento);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,0);
        if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.O){
            NotificationChannel notificationChannel = new NotificationChannel("SafeParty", "SafeParty", NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription("SafeParty notification");
            notificationChannel.setShowBadge(true);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "SafeParty")
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher_round))
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(getApplicationContext().getResources().getString(R.string.event_notification_title))
                .setContentText(getApplicationContext().getResources().getString(R.string.event_notification_text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(3, builder.build());

    }



    /*
     * In questo metodo vengono controllate lo status e le richieste di partecipaze a tutti gli eventi di cui l'utente che sta
     * utilizzando l'app è Admin. Il tutto avvene in un thread essere il più indipendente possibile dal thread dell'app
     * principale. All'invocazione del metodo vengono salvati lo status e gli eventi gestiti dell'utente.
     * Da quel momento, per ogni mezzora si controlla se lo status è cambiato (si invoca sendStatusNotification) o se, per
     * ogni evento gestito, nell'array delle richieste di partecipazione c'è un cambiamento (si invoca sendRequestNotification)
     * */
    @Override
    protected void onHandleWork(@NonNull Intent intent) {
            db.collection("Utenti").document(FirebaseAuth.getInstance().getUid()).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Utente utente = task.getResult().toObject(Utente.class);
                    status = utente.getStatus();
                    for (String IDEvento : utente.getEventiGestiti()) {
                        db.collection("Eventi").document(IDEvento).get().addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful()) {
                                Evento evento = task1.getResult().toObject(Evento.class);
                                eventiGestiti.add(evento);
                            }
                        });
                    }
                }
            });
            while (true) {
                db.collection("Utenti").document(FirebaseAuth.getInstance().getUid()).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Utente utente = task.getResult().toObject(Utente.class);
                        if (utente.getStatus() != status) {
                            status = utente.getStatus();
                            sendStatusNotification();
                        }
                        for (String IDEvento : utente.getEventiGestiti()) {

                            db.collection("Eventi").document(IDEvento).get().addOnCompleteListener(task1 -> {
                                if (task1.isSuccessful()) {
                                    Evento evento = task1.getResult().toObject(Evento.class);
                                    if (!eventiGestiti.contains(evento)) {
                                        eventiGestiti.add(evento);
                                    } else {
                                        int index = eventiGestiti.indexOf(evento);
                                        Evento evento1 = eventiGestiti.get(index);
                                        for (String IDRicheista : evento.getRichieste()) {
                                            if (!evento1.getRichieste().contains(IDRicheista)) {
                                                eventiGestiti.remove(index);
                                                eventiGestiti.add(evento1);
                                                sendRequestNotification(IDEvento);
                                                Log.d("EVENTO", IDEvento);
                                            }
                                        }
                                    }
                                }
                            });
                        }
                    }
                });
                try {
                    Thread.sleep(600000); //ogni 10 min viene fatto il controllo su status e sulle richieste degli eventi gestiti!!
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

    }
    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, NotificationService.class, 20, work);
    }
}