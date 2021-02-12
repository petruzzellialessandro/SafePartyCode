package com.gingerbread.SafeParty.dataClasses;

import java.util.ArrayList;

public class Utente {
    private int status;
    private ArrayList<String> eventiPartecipati;
    private ArrayList<String> eventiRichiesti;
    private ArrayList<String> eventiGestiti;
    private ArrayList<Contatto> contatti;
    private String email;
    private String photoURL;
    private String name;

    public Utente() {
    }

    public Utente(int status, ArrayList<String> eventiPartecipati, ArrayList<String> eventiRichiesti, ArrayList<String> eventiGestiti, ArrayList<Contatto> contatti, String email, String photoUrl, String name) {
        this.status = status;
        this.eventiPartecipati = eventiPartecipati;
        this.eventiRichiesti = eventiRichiesti;
        this.eventiGestiti = eventiGestiti;
        this.contatti = contatti;
        this.email = email;
        this.photoURL = photoUrl;
        this.name = name;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }


    public ArrayList<String> getEventiPartecipati() {
        return eventiPartecipati;
    }

    public void setEventiPartecipati(ArrayList<String> eventiPartecipati) {
        this.eventiPartecipati = eventiPartecipati;
    }

    public ArrayList<String> getEventiGestiti() {
        return eventiGestiti;
    }

    public void setEventiGestiti(ArrayList<String> eventiGestiti) {
        this.eventiGestiti = eventiGestiti;
    }

    public void addEventiGestiti(String evento) {
        this.eventiGestiti.add(evento);
    }
    public void addEventiRichiesti(String evento) {
        this.eventiRichiesti.add(evento);
    }

    public void addEventiPartecipati(String evento) {
        this.eventiPartecipati.add(evento);
    }

    public ArrayList<Contatto> getContatti() {
        return contatti;
    }

    public void setContatti(ArrayList<Contatto> contatti) {
        this.contatti = contatti;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public ArrayList<String> getEventiRichiesti() {
        return eventiRichiesti;
    }

    public void setEventiRichiesti(ArrayList<String> eventiRichiesti) {
        this.eventiRichiesti = eventiRichiesti;
    }
    public void removeFromRequest(String IDEvento){
        this.eventiRichiesti.remove(IDEvento);
    }

    public String getPhotoUrl() {
        return photoURL;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoURL = photoUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
