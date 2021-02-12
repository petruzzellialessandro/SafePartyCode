package com.gingerbread.SafeParty.dataClasses;

import java.util.Date;

public class Contatto {
    private String ID_contatto;
    private Date data_contatto;
    private boolean da_tracciare;

    public Contatto(){}

    public Contatto(String ID_contatto,Date data_contatto, boolean da_tracciare) {
        this.ID_contatto = ID_contatto;
        this.data_contatto = data_contatto;
        this.da_tracciare = da_tracciare;
    }

    public String getID_contatto() {
        return ID_contatto;
    }

    public void setID_contatto(String ID_contatto) {
        this.ID_contatto = ID_contatto;
    }

    public Date getData_contatto() {
        return data_contatto;
    }

    public void setData_contatto(Date data_contatto) {
        this.data_contatto = data_contatto;
    }


    public boolean isDa_tracciare() {
        return da_tracciare;
    }

    public void setDa_tracciare(boolean da_tracciare) {
        this.da_tracciare = da_tracciare;
    }
}
