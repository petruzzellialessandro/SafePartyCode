package com.gingerbread.SafeParty.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class ServerThread extends Thread{

    BluetoothAdapter bluetoothAdapter;
    private BluetoothServerSocket mmServerSocket;
    UUID uuid = UUID.fromString("00001101-0200-1200-1999-11805f9b34fb");
    private final Context context;

    /*
     * Costruttore della classe server. Operazione principale di questo metodo è quello di rendere
     * disponibile il servizio identificato dall'UUID. Il servizio è "offerto" in maniera insicura ovvero
     * il client può connettersi senza effettuare il pairing.
     * */
    public ServerThread(BluetoothAdapter bluetoothAdapter, Context context){
        BluetoothServerSocket tmp = null;
        this.context = context;
        this.bluetoothAdapter = bluetoothAdapter;
        try {
            tmp = this.bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("SafeParty", uuid);
        } catch (IOException e) {
        }
        mmServerSocket = tmp;
    }

    /*
     * Con questo metodo si permette alla classe Server di restare sempre in ascolto. Quando un device
     * prova a connetersi viene istanziato un oggetto della classe SendReceiver.
     * Tutte le eccezioni sollevate non sono gestite poiché riguardano tenentativi di connessione non
     * andati a buon fine o di cui si è persa la connessione. In questo non è necessario effettuare operazioni aggiuntive
     * */
    @Override
    public void run() {
        super.run();
        BluetoothSocket socket;
        while (true){
            try {
                socket = mmServerSocket.accept();
            } catch (IOException e) {
                break;
            }

            if (socket != null) {
                SendReceiver sendReceiver = new SendReceiver(socket, this.context);
                sendReceiver.start();
                break;
            }
        }
    }

    private class SendReceiver extends Thread{
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;
        private final Context context;

        /*
         * Questa classe si occupa di ricevere le informazioni dalla socket.
         * Utilizza i flussi input e output e, tramite un costrutto
         * try-catch, memorizza le informazioni della bluetoothSocket
         * nelle rispettive variabili temporanee.
         * */
        public SendReceiver(BluetoothSocket socket, Context context)  {
            this.bluetoothSocket = socket;
            this.context = context;
            InputStream tempIn = null;
            OutputStream tempOUT = null;
            try {
                tempIn = bluetoothSocket.getInputStream();
                tempOUT = bluetoothSocket.getOutputStream();
            }catch (Exception e){
            }
            inputStream = tempIn;
            outputStream = tempOUT;
        }

        /*
         * In questo metodo si invia, salvo perdite di connessione, alla socket del client ID identificativo
         * dell'utente. ID si cerca in primi nelle SharedPreferences. Se non è presente (cosa poca probabile)
         * allora si accede all'UID direttamente da FirebaseAuth
         * */
        @Override
        public void run() {
            super.run();
            SharedPreferences preferences = this.context.getSharedPreferences("com.gingerbread.SafeParty", Context.MODE_PRIVATE);
            String UID_Utente = preferences.getString("UID_Utente", "Not_defined");
            if (UID_Utente.equals("Not_defined"))
            {
                UID_Utente = FirebaseAuth.getInstance().getUid();
            }
            write(UID_Utente.getBytes());
        }
        public void write(byte[] bytes)
        {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
            }
        }
    }
}