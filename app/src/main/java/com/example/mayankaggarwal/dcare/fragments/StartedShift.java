package com.example.mayankaggarwal.dcare.fragments;


import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.mayankaggarwal.dcare.R;
import com.example.mayankaggarwal.dcare.rest.Data;
import com.example.mayankaggarwal.dcare.utils.AlarmReciever;
import com.example.mayankaggarwal.dcare.utils.Globals;
import com.example.mayankaggarwal.dcare.utils.Prefs;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONObject;

import java.util.GregorianCalendar;

import static com.example.mayankaggarwal.dcare.R.drawable.context;


/**
 * A simple {@link Fragment} subclass.
 */
public class StartedShift extends Fragment {

    Button endShift;
    TextView vendorname, orderack, orderpending;
    Fragment fragment;
    public String latitude;
    public String longitude;
    private static final int REQUEST_PERMISSION = 1;
    Activity activity;
    Context context;

    public static StartedShift newInstance() {
        StartedShift fragment = new StartedShift();
        return fragment;
    }

    public StartedShift() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        activity=getActivity();
        context=getContext();
        getCurrentLocation();
        View view = inflater.inflate(R.layout.fragment_started_shift, container, false);
        endShift = (Button) view.findViewById(R.id.nodialog);
        vendorname = (TextView) view.findViewById(R.id.vendor_name);
        orderack = (TextView) view.findViewById(R.id.ordersack);
        orderpending = (TextView) view.findViewById(R.id.orderspending);
        if (!(Prefs.getPrefs("vendor_id_name", context).equals("notfound"))) {
            vendorname.setText(Prefs.getPrefs("vendor_id_name", context));
        }
        scheduleShiftAlarm(context);
        scheduleLocalShiftAlarm(context);
        if(Globals.orderFetch==0){
            getOrder(getContext(),activity);
        }else {
            try {
                if (!(Prefs.getPrefs("orderJson", getContext())).equals("notfound")) {
                    int ack = 0, pending = 0;
                    JsonParser jsonParser = new JsonParser();
                    JsonObject ob = jsonParser.parse(Prefs.getPrefs("orderJson", getContext())).getAsJsonObject();
                    JsonArray orderArray = ob.get("payload").getAsJsonObject().get("orders").getAsJsonObject().get("orders").getAsJsonArray();
                    for (int i = 0; i < orderArray.size(); i++) {
                        JsonObject orderObject = orderArray.get(i).getAsJsonObject().get("order").getAsJsonObject();
                        String order_code = orderObject.get("order_last_state_code").getAsString();
                        if (Integer.parseInt(order_code) == Globals.ORDERSTATE_ASSIGNED) {
                            pending++;
                        } else if (Integer.parseInt(order_code) == Globals.ORDERSTATE_CREW_AKNOLEDGED) {
                            ack++;
                        }
                    }
                    orderack.setText(String.valueOf(ack));
                    orderpending.setText(String.valueOf(pending));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        setListener();
        Log.d("tagg", Prefs.getPrefs("vendor_id_selected", context));
        Log.d("tagg", Prefs.getPrefs("wpr_token", context));
        Log.d("tagg", Prefs.getPrefs("activity_list_selected", context));
        Log.d("tagg", Prefs.getPrefs("shift_id", context));
        Log.d("tagg", Prefs.getPrefs("crewid", context));
        return view;
    }

    private void validateCrewShift() {
            Data.validateShift(context, Prefs.getPrefs("vendor_id_selected", context), Prefs.getPrefs("shift_id", context), Globals.lat, Globals.lng, new Data.UpdateCallback() {
                @Override
                public void onUpdate() {
                    Log.d("tagg", "success validating shift");
                    Globals.validatedShift = 1;
                }

                @Override
                public void onFailure() {
                    Prefs.setPrefs("shiftStarted", "0", context);
                    Prefs.setPrefs("trip_started", "0", context);

                    fragment = ShiftFragment.newInstance();
                    FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                    transaction.replace(R.id.frame_layout, fragment);
                    transaction.commit();
                }
            });
    }

    public static void scheduleLocalShiftAlarm(Context context) {
        int time = Integer.parseInt(Prefs.getPrefs("local_shift_refresh_frequency_rate", context)) * 1000;
        Intent intentAlarm = new Intent(context, AlarmReciever.class);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, time,
                PendingIntent.getBroadcast(context, 1, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT));
    }


    public static void scheduleShiftAlarm(Context context) {
        if (!(Prefs.getPrefs("shift_refresh_frequency_rate", context).equals("notfound"))) {
            int time = Integer.parseInt(Prefs.getPrefs("shift_refresh_frequency_rate", context)) * 1000;
            Intent intentAlarm = new Intent(context, AlarmReciever.class);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.set(AlarmManager.RTC_WAKEUP, time,
                    PendingIntent.getBroadcast(context, 1, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT));
        }
    }

    private void setListener() {
        endShift.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        endShift.setBackgroundResource(R.drawable.round_shape_solid_invalid);
                        endShift.setTextColor(getResources().getColor(R.color.white));
                        return false;
                    case MotionEvent.ACTION_UP:
                        endShift.setBackgroundResource(R.drawable.round_shape_border_orange);
                        endShift.setTextColor(getResources().getColor(R.color.themered));
                        return false;
                }
                return false;
            }
        });
        endShift.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String vendor_id = Prefs.getPrefs("vendor_id_selected", context);
                String checkItems_id = Prefs.getPrefs("activity_list_selected", context);
                Data.crewShiftStartEnd(activity, vendor_id, checkItems_id, "end", Globals.lat, Globals.lng, new Data.UpdateCallback() {
                    @Override
                    public void onUpdate() {
                        Prefs.setPrefs("shiftStarted", "0", context);
                        Prefs.setPrefs("trip_started", "0", context);
                        fragment = ShiftFragment.newInstance();
                        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                        transaction.replace(R.id.frame_layout, fragment);
                        transaction.commit();
                    }

                    @Override
                    public void onFailure() {
                        Globals.showFailAlert(activity, "Error Ending Shift!");
                    }
                });
            }
        });
    }

    public void getCurrentLocation() {
        final LocationManager locationManager;
        LocationListener locationListener = null;
        locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSION);
            return;
        }


        final LocationListener finalLocationListener = locationListener;
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (location == null) {
                    if (locationManager != null) {
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return;
                        }
                        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, finalLocationListener);
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, finalLocationListener);
                    }
                } else {
                    latitude = String.valueOf(location.getLatitude());
                    longitude = String.valueOf(location.getLongitude());
                    Globals.lat = String.valueOf(location.getLatitude());
                    Globals.lng = String.valueOf(location.getLongitude());
                    if (Globals.validatedShift == 0) {
                        validateCrewShift();
                    }
                }
//                Log.d("tagg", "lat:" + latitude);
//                Log.d("tagg", "lng:" + longitude);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

    }

    public void getOrder(final Context context,final Activity activity) {
        if (!(Prefs.getPrefs("vendor_id_selected", context).equals("notfound")) && !(Prefs.getPrefs("shift_id", context).equals("notfound"))) {
            Data.getAllOrders(activity, Prefs.getPrefs("vendor_id_selected", context), Prefs.getPrefs("shift_id", context), new Data.UpdateCallback() {
                @Override
                public void onUpdate() {
                    Log.d("tagg", "success order");
                    try {
                        if (!(Prefs.getPrefs("orderJson", context)).equals("notfound")) {
                            int ack = 0, pending = 0;
                            JsonParser jsonParser = new JsonParser();
                            JsonObject ob = jsonParser.parse(Prefs.getPrefs("orderJson", context)).getAsJsonObject();
                            JsonArray orderArray = ob.get("payload").getAsJsonObject().get("orders").getAsJsonObject().get("orders").getAsJsonArray();
                            for (int i = 0; i < orderArray.size(); i++) {
                                JsonObject orderObject = orderArray.get(i).getAsJsonObject().get("order").getAsJsonObject();
                                String order_code = orderObject.get("order_last_state_code").getAsString();
                                if (Integer.parseInt(order_code) == Globals.ORDERSTATE_ASSIGNED) {
                                    pending++;
                                } else if (Integer.parseInt(order_code) == Globals.ORDERSTATE_CREW_AKNOLEDGED) {
                                    ack++;
                                }
                            }
                            orderack.setText(String.valueOf(ack));
                            orderpending.setText(String.valueOf(pending));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure() {
                    Globals.showFailAlert(activity, "Error fetching orders!");
                }
            });
        }
    }

}
