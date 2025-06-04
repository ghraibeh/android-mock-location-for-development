package amotz.example.com.mocklocationfordeveloper;

import com.google.android.gms.maps.model.LatLng;
import java.util.List;

public class PathModel {
    public String name;
    public List<LatLng> points;

    public PathModel(String name, List<LatLng> points) {
        this.name = name;
        this.points = points;
    }
}
