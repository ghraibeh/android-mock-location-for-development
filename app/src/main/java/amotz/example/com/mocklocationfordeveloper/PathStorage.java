package amotz.example.com.mocklocationfordeveloper;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class PathStorage {
    private static final String PREF_NAME = "paths_prefs";
    private static final String KEY_PATHS = "paths";

    private SharedPreferences prefs;
    private Gson gson = new Gson();

    public PathStorage(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public List<PathModel> getPaths() {
        String json = prefs.getString(KEY_PATHS, "[]");
        Type listType = new TypeToken<List<PathModel>>(){}.getType();
        List<PathModel> paths = gson.fromJson(json, listType);
        if (paths == null) return new ArrayList<>();
        return paths;
    }

    public void savePath(PathModel path) {
        List<PathModel> paths = getPaths();
        paths.add(path);
        prefs.edit().putString(KEY_PATHS, gson.toJson(paths)).apply();
    }
}
