package com.gingerbread.SafeParty.bluetooth;

import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.gingerbread.contagiapp.R;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Set;

public class BackgroundBluetoothService extends JobIntentService {
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private HashMap<String, BluetoothDevice> mapDevices = new HashMap<String, BluetoothDevice>();
    ServerThread serverThread;
    private BroadcastReceiver scanReceiver;

    /*
     * Questo BroadcastReceiver si occupa di gestire le operazioni da effettuare
     * nel momento in cui cambia lo stato del Bluetooth, ricevendo un'azione
     * effettuata, la quale viene confrontata con una broadcast action (chiamata
     * ACTION_STATE_CHANGED) del BluetoothAdapter. Gestendo il solo caso del
     * Bluetooth disattivato, vengono chiamati in ordine i seguendi metodi:
     * 1)il metodo della notifica da mostrare a schermo per la sua riattivazione,
     * 2)il metodo per la vera e propria riattivazione del Bluetooth
     * 3)il metodo che avvia la scansione dei dispositivi vicini.
     * */
    private BroadcastReceiver broadcastReceiver;

    /*
     * Questo metodo serve per verificare che la visibilità del bluetooh di un dispositivo è abilitata.
     * Poiché nelle versioni più recenti di Android non è più possibile chiedere all'untente di essere
     * sempre visibili, in questo modeto si usano una serie di istruzioni che forzano questo "limite".
     * Questa soluzione è definita come "insicura" e potrebbe sollevare una eccezione che, volutamente,
     * non è gestita poiché l'utente dovrebbe ricevere la notifica se il bluetooth non è visibile.
     * */
    public void enableBluetoothVisibility() {
        try {
            Method bluetoothDeviceVisibility;
            bluetoothDeviceVisibility = bluetoothAdapter.getClass().getMethod("setScanMode", int.class, int.class);
            bluetoothDeviceVisibility.invoke(bluetoothAdapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, 0);
        } catch (Exception e) {
        }
    }

    /*
     * Questo metodo serve ad inviare una notifica bluetooth nel caso in cui la visibilità del
     * dispositivo non sia attiva, e, perciò, viene chiesto di attivarla. L'If viene inserito nel
     * metodo per contollare la versione dell'SDK, dato che dalla versione API 26 ci sono delle
     * differenze.
     * */
    private void notificationBt() {
        Intent enableBt = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, enableBt, 0);
        enableBt.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel("SafeParty", "SafeParty", NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription("SafeParty notification");
            notificationChannel.setShowBadge(true);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "SafeParty")
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getApplicationContext().getResources().getString(R.string.bt_notification_title))
                .setContentText(getApplicationContext().getResources().getString(R.string.bt_notification_text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(1, builder.build());
    }

    /*
     * Come prima cosa questo metodo aggiunge nalla mappa dei dispositivi trovati i device con cui in precedenza
     * si è effettuato il pairing. In seguito "parte" un altro thread con cui si cercano tutt ii device non paired.
     * Quando se ne trova uno si aggiunge alla mappa dei dispositivi trovati e si fa partire un client thread.
     * E' chiaro che non tutti i dispositivi trovati accettano connessioni quindi questo potrebbe sollevare una
     * eccezione. Questo non è però un problema, il problema non vva segnalato all'untente (non deve sapere se
     * il bluetooth è connesso ad un dispostivo o meno) e non devono esserci ripecussioni sulla vita dell'app.
     * Questa operazione avviene a cadenza di 20 secondi.
     * */
    public void scan() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (mapDevices.get(device.getAddress()) == null) {
                    mapDevices.put(device.getAddress(), device);
                }
            }
        }
        new Thread(() -> {
            while (true) {
                boolean val = bluetoothAdapter.startDiscovery();
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(scanReceiver, filter);
                scanReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String action = intent.getAction();
                        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                            if (mapDevices.get(device.getAddress()) == null) {
                                mapDevices.put(device.getAddress(), device);
                                new ClientThread(device, bluetoothAdapter, getApplicationContext()).start();
                            }
                        }
                    }
                };
                try {
                    Thread.sleep(20000);
                } catch (InterruptedException e) {
                }
            }
        }).start();
    }

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, BackgroundBluetoothService.class, 19, work);
    }



    /* Nella onHandleWork() inizialmente avviamo il receiver e il metodo che consente di abilitare la visibilità Bluetooth,
     * subito dopo viene creato il thread adibito alla scansione per la rilevazione dei dispositivi vicini.
     * Il thread infatti fa partire il metodo scan e ogni ora pulisce la mappa dei contatti riempita
     * da scan. Infine viene creata e fatta partire un'istanza di ServerThread.
     * */

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    switch (state) {
                        case BluetoothAdapter.STATE_OFF:
                            notificationBt();
                            break;
                        case BluetoothAdapter.STATE_ON:
                            enableBluetoothVisibility();
                            scan();
                            break;
                    }
                } else {
                    if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
                        final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);
                        switch (state) {
                            case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                            case BluetoothAdapter.SCAN_MODE_NONE:
                                enableBluetoothVisibility();
                                break;
                        }
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(broadcastReceiver, filter);
        enableBluetoothVisibility();
        serverThread = new ServerThread(this.bluetoothAdapter, this);
        serverThread.start();
        while (true) {
            scan();
            try {
                Thread.sleep(3600000); //ogni ora svuota la lista di device trovati!!
            } catch (InterruptedException e) {
            }
            mapDevices.clear();
        }

    }
}