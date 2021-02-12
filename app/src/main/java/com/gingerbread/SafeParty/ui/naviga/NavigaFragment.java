package com.gingerbread.SafeParty.ui.naviga;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryBounds;
import com.gingerbread.SafeParty.ui.event.EventActivity;
import com.gingerbread.contagiapp.R;
import com.gingerbread.SafeParty.dataClasses.Evento;
import com.gingerbread.SafeParty.ui.event.adapter.EventCardAdapter;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NavigaFragment extends Fragment {

    private Spinner distance;
    private ArrayAdapter<String> spinnerAdapter;
    private ArrayList<Evento> eventFoundList;
    private ArrayList<String> eventFoundID;
    private EventCardAdapter eventFoundAdapter;
    private ListView nearEvent;
    private FusedLocationProviderClient fusedLocationClient;
    private double userLat;
    private double userLon;
    private int distanceKM = 5;
    private ProgressBar progressBar;


    @SuppressLint("MissingPermission")//I permessi sono chiesti nell'activity HOME.
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);
        View root = inflater.inflate(R.layout.fragment_naviga, container, false);
        nearEvent = root.findViewById(R.id.near_event);
        nearEvent.setVisibility(View.GONE);
        distance = root.findViewById(R.id.distance);
        progressBar = root.findViewById(R.id.loadingEvent);
        spinnerAdapter = new ArrayAdapter<String>(getActivity().getApplicationContext(), android.R.layout.simple_dropdown_item_1line, new String[]{"5KM", "10KM", "20KM", "50KM"});
        distance.setAdapter(spinnerAdapter);
        distance.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        distanceKM = 5;
                        progressBar.setVisibility(View.VISIBLE);
                        nearEvent.setVisibility(View.GONE);
                        break;
                    case 1:
                        distanceKM = 10;
                        progressBar.setVisibility(View.VISIBLE);
                        nearEvent.setVisibility(View.GONE);
                        break;
                    case 2:
                        distanceKM = 20;
                        progressBar.setVisibility(View.VISIBLE);
                        nearEvent.setVisibility(View.GONE);
                        break;
                    case 3:
                        distanceKM = 50;
                        progressBar.setVisibility(View.VISIBLE);
                        nearEvent.setVisibility(View.GONE);
                        break;
                }
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(getActivity(), location -> {
                            if (location != null) {
                                userLat = location.getLatitude();
                                userLon = location.getLongitude();
                                getNearEvent();
                            }
                        });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        return root;
    }

    public void getNearEvent() {
        eventFoundID = new ArrayList<>();
        eventFoundList = new ArrayList<Evento>();
        eventFoundAdapter = new EventCardAdapter(getActivity().getApplicationContext(), android.R.layout.simple_list_item_1, eventFoundList);
        nearEvent.setAdapter(eventFoundAdapter);
        GeoLocation center = new GeoLocation(this.userLat, this.userLon);
        double raggio = distanceKM * 1000;
        List<GeoQueryBounds> bounds = GeoFireUtils.getGeoHashQueryBounds(center, raggio);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        for (GeoQueryBounds b : bounds) {
            db.collection("Eventi")
                    .orderBy("geoHash")
                    .orderBy("data")
                    .whereEqualTo("privato", false)
                    .startAt(b.startHash).endAt(b.endHash)
                    .get()
                    .addOnCompleteListener(task -> {
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            double lat = doc.getDouble("lat");
                            double lng = doc.getDouble("lon");
                            GeoLocation docLocation = new GeoLocation(lat, lng);
                            double distanceInM = GeoFireUtils.getDistanceBetween(docLocation, center);
                            Evento evento = doc.toObject(Evento.class);
                            if (distanceInM <= raggio && evento.getData().after(new Date())) {
                                eventFoundID.add(doc.getId());
                                eventFoundAdapter.add(evento);
                                eventFoundAdapter.notifyDataSetChanged();
                                progressBar.setVisibility(View.GONE);
                                nearEvent.setVisibility(View.VISIBLE);
                            }
                        }
                        nearEvent.setOnItemClickListener((parent, view, position, id) -> {
                            Intent intent = new Intent(getActivity(), EventActivity.class);
                            intent.putExtra("ID_Evento", eventFoundID.get(position));
                            startActivity(intent);
                        });
                    });
        }
    }

}