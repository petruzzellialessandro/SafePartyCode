package com.gingerbread.SafeParty.dataClasses;

import androidx.annotation.Nullable;

import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;

public class Evento implements Comparator {
    private String nome;
    private String descrizione;
    private int numPartecipanti;
    private boolean privato;
    private Date data;
    private String via;
    private String città;
    private String regione;
    private String nazione;
    private double lat;
    private double lon;
    private int statusRichiesto;
    private ArrayList<String> gestori;
    private ArrayList<String> partecipanti;
    private ArrayList<String> richieste;
    private String geoHash;


    @Override
    public boolean equals(@Nullable Object obj) {
        if ((nome.equals(((Evento) obj).nome)) ||
                ((descrizione.equals(((Evento) obj).descrizione))) ||
                ((via.equals(((Evento) obj).via))) ||
                ((città.equals(((Evento) obj).città))) ||
                ((regione.equals(((Evento) obj).regione))) ||
                ((nazione.equals(((Evento) obj).nazione))) ||
                ((geoHash.equals(((Evento) obj).geoHash))) ||
                ((numPartecipanti == (((Evento) obj).numPartecipanti))) ||
                ((privato == (((Evento) obj).privato))) ||
                ((lat == (((Evento) obj).lat))) ||
                ((lon == (((Evento) obj).lon))) ||
                ((statusRichiesto == (((Evento) obj).statusRichiesto)))
        ) {
            return true;
        }
        return false;
    }

    public Evento() {
    }

    public Evento(String nome, String descrizione, int numPartecipanti, boolean privato, Date data, String via, String città, String regione, String nazione, double lat, double lon, ArrayList<String> gestori, ArrayList<String> partecipanti, ArrayList<String> richieste, int statusRichiesto) {
        this.nome = nome;
        this.descrizione = descrizione;
        this.numPartecipanti = numPartecipanti;
        this.privato = privato;
        this.data = data;
        this.via = via;
        this.città = città;
        this.regione = regione;
        this.nazione = nazione;
        this.lat = lat;
        this.lon = lon;
        this.gestori = gestori;
        this.partecipanti = partecipanti;
        this.richieste = richieste;
        this.statusRichiesto = statusRichiesto;
        this.geoHash = GeoFireUtils.getGeoHashForLocation(new GeoLocation(this.lat, this.lon));
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public int getNumPartecipanti() {
        return numPartecipanti;
    }

    public void setNumPartecipanti(int numPartecipanti) {
        this.numPartecipanti = numPartecipanti;
    }

    public boolean isPrivato() {
        return privato;
    }

    public void setPrivato(boolean privato) {
        this.privato = privato;
    }

    public Date getData() {
        return data;
    }

    public void setData(Date data) {
        this.data = data;
    }

    public String getVia() {
        return via;
    }

    public void setVia(String via) {
        this.via = via;
    }

    public String getCittà() {
        return città;
    }

    public void setCittà(String città) {
        this.città = città;
    }

    public String getRegione() {
        return regione;
    }

    public void setRegione(String regione) {
        this.regione = regione;
    }

    public String getNazione() {
        return nazione;
    }

    public void setNazione(String nazione) {
        this.nazione = nazione;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public ArrayList<String> getGestori() {
        return gestori;
    }

    public void setGestori(ArrayList<String> gestori) {
        this.gestori = gestori;
    }

    public ArrayList<String> getPartecipanti() {
        return partecipanti;
    }

    public void setPartecipanti(ArrayList<String> partecipanti) {
        this.partecipanti = partecipanti;
    }

    public void addPartecipante(String partecipante) {
        this.partecipanti.add(partecipante);
    }

    public void addGestore(String gestore) {
        this.gestori.add(gestore);
    }

    public void addRichiesta(String utente) {
        this.richieste.add(utente);
    }

    public void removRequest(String utente) {
        this.richieste.remove(utente);
    }

    public ArrayList<String> getRichieste() {
        return richieste;
    }

    public void setRichieste(ArrayList<String> richieste) {
        this.richieste = richieste;
    }

    public int getStatusRichiesto() {
        return statusRichiesto;
    }

    public void setStatusRichiesto(int statusRichiesto) {
        this.statusRichiesto = statusRichiesto;
    }

    public String getGeoHash() {
        return geoHash;
    }

    public void setGeoHash(String geoHash) {
        this.geoHash = geoHash;
    }

    @Override
    public int compare(Object o1, Object o2) {
        if (((Evento) o1).getData().before(((Evento) o2).data)) {
            return 1;
        } else {
            return -1;
        }
    }
}
