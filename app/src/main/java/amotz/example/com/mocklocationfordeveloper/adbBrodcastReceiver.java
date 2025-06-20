package amotz.example.com.mocklocationfordeveloper;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.location.LocationManager;
import android.util.Log;

public class adbBrodcastReceiver extends BroadcastReceiver {

    MockLocationProvider mockGPS;
    MockLocationProvider mockWifi;
    String logTag="MockGpsadbBrodcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {



        if (intent.getAction().equals("stop.mock")) {
            if (mockGPS != null) {
                mockGPS.shutdown();
                mockGPS = null;
            }
            if (mockWifi != null) {
                mockWifi.shutdown();
                mockWifi = null;
            }
        }
        else {
            if (mockGPS == null) {
                mockGPS = new MockLocationProvider(LocationManager.GPS_PROVIDER, context);
            }
            if (mockWifi == null) {
                mockWifi = new MockLocationProvider(LocationManager.NETWORK_PROVIDER, context);
            }

            double lat, lon, alt;
            float accurate;

            lat = Double.parseDouble(intent.getStringExtra("lat") != null ? intent.getStringExtra("lat") : "0");
            lon = Double.parseDouble(intent.getStringExtra("lon") != null ? intent.getStringExtra("lon") : "0");
            alt = Double.parseDouble(intent.getStringExtra("alt") != null ? intent.getStringExtra("alt") : "0");
            accurate = Float.parseFloat(intent.getStringExtra("accurate") != null ? intent.getStringExtra("accurate") : "0");
            Log.i(logTag, String.format("setting mock to Latitude=%f, Longitude=%f Altitude=%f Accuracy=%f", lat, lon, alt, accurate));
            mockGPS.pushLocation(lat, lon, alt, accurate);
            mockWifi.pushLocation(lat, lon, alt, accurate);
        }
    }
}
