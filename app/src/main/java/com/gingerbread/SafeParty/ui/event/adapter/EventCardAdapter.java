package com.gingerbread.SafeParty.ui.event.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gingerbread.contagiapp.R;
import com.gingerbread.SafeParty.dataClasses.Evento;

import java.util.ArrayList;
import java.util.Date;

public class EventCardAdapter extends ArrayAdapter<Evento> {
    public EventCardAdapter(@NonNull Context context, int resource, ArrayList<Evento> eventFoundList) {
        super(context, resource);

    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Evento evento = getItem(position);
        if (convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_event_card, parent, false);
        }
        TextView event_name = convertView.findViewById(R.id.discover_event_name);
        TextView event_date = convertView.findViewById(R.id.discover_event_date);
        TextView event_hour = convertView.findViewById(R.id.discover_event_hour);
        TextView event_place = convertView.findViewById(R.id.discover_event_place);


        event_name.setText(evento.getNome());
        event_date.setText(getDataToPrint(evento.getData()));
        String ora = String.format("%02d",evento.getData().getHours() );
        String minuti = String.format("%02d",evento.getData().getMinutes() );
        event_hour.setText(ora + ":" + minuti);
        event_place.setText(evento.getCittà());
        return convertView;
    }
    private String getDataToPrint(Date date){
        String giorno = String.format("%02d",date.getDate() );
        String mese = String.format("%02d",(date.getMonth() + 1) );
        String anno = String.format("%04d",(date.getYear()+1900) );
        return (giorno + "/" + mese + "/" + anno);
    }
    private String getEventToPrint(Evento evento){
        return "\n"  + evento.getNome() + "\n" + getContext().getString(R.string.event_date_list) + "  " +getDataToPrint(evento.getData()) + "\n" + getContext().getString(R.string.event_city_list) + "  " + evento.getCittà()  + "\n";
    }
}
