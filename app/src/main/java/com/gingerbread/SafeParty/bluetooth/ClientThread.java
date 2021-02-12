package com.gingerbread.SafeParty.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.UUID;

public class ClientThread extends Thread {
    private BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private UUID uuid = UUID.fromString("00001101-0200-1200-1999-11805f9b34fb");
    BluetoothAdapter bluetoothAdapter;
    Context context;

    /*
    * Questo metodo, grazie al codice alfanumerico UUID, sa immediatamente se il dispositivo con il
    * quale siamo in connessione può ricambiare la nostra richiesta. Infatti questo codice indentifica
    * univocamente il servizio offerto dall'app, inoltre la connessione stabilita fra il nostro device, e quello appena incontrato,
    * non necessita di un'autorizzazione da aprte dell'utente grazie a 'device.createInsecureRfcommSocketToServiceRecord(uuid)'.
    * Al termine dell'operazione, memorizziamo la soket del device incontrato nella variabile mmSocket che ci servirà più avanti.
     * */
    public ClientThread(BluetoothDevice device, BluetoothAdapter bluetoothAdapter, Context context) {
        this.context = context;
        this.bluetoothAdapter = bluetoothAdapter;
        BluetoothSocket tmp = null;
        mmDevice = device;

        try {
            tmp = device.createInsecureRfcommSocketToServiceRecord(uuid);
        } catch (IOException e) {
        }
        mmSocket = tmp;
    }

    /*
     * Questo metodo serve per connettere il dispositivo attraverso la socket bluetooth. Nel caso in
     * cui la connessione vada a buon fine, viene istanziato un oggetto di classe SendReceiver(),
     * altrimenti la connessione viene chiusa. Nel caso in cui vengono sollevate delle eccezioni
     * non vengono gestite perchè riguardano perdite di connessione e non sono operazioni necessarie.
     * */
    public void run() {
        try {
            mmSocket.connect();
            SendReceiver sendReceiver = new SendReceiver(mmSocket, context);
            sendReceiver.start();
        } catch (IOException connectException) {
            try {
                mmSocket.close();
            } catch (IOException closeException) {
            }
            return;
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
        * Questo thread si occupa di creare il messaggio contenente l'ID utente e
        * la data, che i dispositivi si invieranno tramite la socket, memorizzandolo
        * su file. Controlla, innanzitutto, che la  cartella ospite del file ci sia,
        * e, nel caso in cui l'esito sia negativo, procede alla sua creazione.
        * Proseguendo, crea il file (ContactData.dat) nella directory selezionata
        * e istanzia un oggetto di classe FileWriter (responsabile della scrittura
        * di file composti da caratteri). Dopodiché, procede alla scrittura del
        * messaggio su file, seguita dalla data in cui quest'ultimo viene scambiato
        * (convertendola in una stringa). Il tutto viene gestito tramite un costrutto
        * try-catch
        * */
        @Override
        public void run() {
            super.run();
            byte[] buffer = new byte[1024];
            int bytes;
            while (true){
                try {
                    bytes = inputStream.read(buffer);
                    String messaggio = new String(buffer, 0, bytes);
                    File dir = new File(context.getFilesDir(), "/SafeParty");
                    if(!dir.exists()){
                        dir.mkdir();
                    }
                    File gpxfile = new File(dir, "ContactData.dat");
                    FileWriter writer = new FileWriter(gpxfile);
                    writer.append(messaggio+ ","+ new Date().toString()+ ";\n");
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
