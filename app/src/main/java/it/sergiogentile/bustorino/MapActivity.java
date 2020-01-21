package it.sergiogentile.bustorino;

import android.app.Dialog;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.vision.barcode.Barcode;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;

import android.location.LocationListener;
import android.location.LocationManager;
import android.text.Html;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import it.sergiogentile.bustorino.model.BusPosition;
import it.sergiogentile.bustorino.model.StopPosition;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {


    public static final String REQUEST_METHOD = "GET";
    public static final int READ_TIMEOUT = 15000;
    public static final int CONNECTION_TIMEOUT = 15000;
    public List<BusPosition> busPositions;
    public List<StopPosition> stopPositions;
    public GoogleMap map;
    public List<Marker> busMarkers;
    public List<Marker> stopMarkers;

    private final Semaphore available = new Semaphore(1, true);
    public String line="1";
    public String mainUrl = "https://falco.5t.torino.it/?option=com_jumi&fileid=4&Itemid=104&jform%5Bsho%5D=l&jform%5Bmode%5D=0&jform%5Bval%5D=";
    public String passaggiUrl = "http://www.gtt.to.it/cms/index.php?option=com_gtt&task=palina.getTransitiOld&bacino=U&realtime=true&get_param=value&palina=";
    public String downloadKey = "";
    public ProgressBar progressBar;
    public Dialog mBottomSheetDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        mBottomSheetDialog = new Dialog(MapActivity.this, R.style.MaterialDialogSheet);
        mBottomSheetDialog.setContentView(R.layout.custom_dialog); // your custom view.
        mBottomSheetDialog.setCancelable(true);
        mBottomSheetDialog.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mBottomSheetDialog.getWindow().setGravity(Gravity.BOTTOM);
        mBottomSheetDialog.hide();

        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Download Key URL
        Thread downloadUrl = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean tryGetKey = true;
                while(tryGetKey){
                    try {
                        String pageKey = downloadPage(mainUrl + line);
                        downloadKey = extractKeyDownload(pageKey);
                        if(downloadKey!=null && !downloadKey.equals("")){
                            tryGetKey = false;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        downloadUrl.start();


        busPositions = new ArrayList<>();
        busMarkers = new ArrayList<>();
        stopMarkers = new ArrayList<>();
        stopPositions = new ArrayList<>();

        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.bus_array, android.R.layout.simple_spinner_item);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                progressBar.setVisibility(View.VISIBLE);
                line = parent.getItemAtPosition(position).toString();
                for(Marker m: busMarkers){
                    m.remove();
                }
                busMarkers.clear();

                for(Marker m: stopMarkers){
                    m.remove();
                }
                stopMarkers.clear();
                stopPositions.clear();

                //download bus stop
                Thread downloadStop = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final String downloadPage = downloadPage(mainUrl + line);
                            for(String row: downloadPage.split(";")){
                                if(row.contains("fermata")){
                                    row = row.replace("fermata({", "").replace("})", "");
                                    StopPosition sp = new StopPosition();
                                    for(String item: row.split(",")){
                                        try{
                                            String[] items = item.split("\":");
                                            String key = items[0].replace("\"","");
                                            String value = items[1].replace("\"","");
                                            //Set parameters
                                            if(key.equals("id")){
                                                sp.setNumFermata(Integer.valueOf(value));
                                            }
                                            if(key.equals("lat")){
                                                sp.setLat(Double.valueOf(value));
                                            }
                                            if(key.equals("lng")){
                                                sp.setLng(Double.valueOf(value));
                                            }
                                            if(key.equals("ico")){
                                                sp.setDir(value);
                                            }
                                            if(key.equals("name")){
                                                sp.setName(value);
                                            }
                                            if(key.equals("addr")) {
                                                sp.setAddress(value);
                                            }
                                        }
                                        catch (Exception e){

                                        }
                                    }
                                    stopPositions.add(sp);
                                }

                            }

                            for(final StopPosition sp: stopPositions){
                                runOnUiThread(new Runnable(){
                                    public void run(){
                                        try {

                                            int height = 70;
                                            int width = 30;
                                            Resources resources = getApplicationContext().getResources();
                                            int resourceId = resources.getIdentifier("fermata" + sp.getDir() , "drawable",
                                                    getApplicationContext().getPackageName());
                                            if(resourceId == 0){
                                                resourceId = resources.getIdentifier("fermata" , "drawable",
                                                        getApplicationContext().getPackageName());
                                            }

                                            BitmapDrawable bitmapdraw = (BitmapDrawable)resources.getDrawable(resourceId);
                                            Bitmap b = bitmapdraw.getBitmap();
                                            Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);


                                            LatLng busPosition = new LatLng(sp.getLat(), sp.getLng());
                                            Marker markerName = map.addMarker(new MarkerOptions().position(busPosition).title(sp.getNumFermata() + " - " + sp.getName() + ""));
                                            markerName.setIcon(BitmapDescriptorFactory.fromBitmap(smallMarker));

                                            stopMarkers.add(markerName);

                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }

                                    }
                                });
                            }



                            final LatLng Turin = getCenterPosition(stopPositions);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    map.moveCamera(CameraUpdateFactory.newLatLng(Turin));
                                    map.animateCamera(CameraUpdateFactory.zoomTo(13), 2000, null);
                                }
                            });


                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

                downloadStop.start();

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        Thread tLoop = new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    while(true){
                        busPositions.clear();
                        String result = downloadPage("https://falco.5t.torino.it/index.php?option=com_jumi&fileid=3&sho=l&k=" + downloadKey +"&val=" + line);
                        for(String row: result.split(";")){
                            if(row.contains("mezzo")){
                                BusPosition bp = new BusPosition();
                                row = row.replace("mezzo", "").replace("({", "").replace("})", "");

                                for(String item: row.split(",")){
                                    String[] items = item.split("\":");
                                    String key = items[0].replace("\"","");
                                    String value = items[1].replace("\"","");
                                    //Set parameters
                                    if(key.equals("mat")){
                                        bp.setNumMezzo(Integer.valueOf(value));
                                    }
                                    if(key.equals("lat")){
                                        bp.setLatitude(Double.valueOf(value));
                                    }
                                    if(key.equals("lng")){
                                        bp.setLongitude(Double.valueOf(value));
                                    }
                                    if(key.equals("v")){
                                        bp.setSpeed(Integer.valueOf(value));
                                    }
                                    if(key.equals("x")){
                                        bp.setHour(value);
                                    }
                                    if(key.equals("dir")){
                                        bp.setDir(value);
                                    }

                                }
                                busPositions.add(bp);
                            }
                        }

                        //System.out.println("Collect " + busPositions.size() + " bus");
                        for(final BusPosition bp: busPositions){
                            runOnUiThread(new Runnable(){
                                public void run(){
                                    try {

                                        //available.acquire();
                                        addMarker(bp);
                                        //available.release();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                }
                            });
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.GONE);
                            }
                        });
                        Thread.sleep(3000);
                    }



                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        tLoop.start();



    }

    private static String downloadPage(String url) throws Exception {
        try{
            URL myUrl = null;
            String inputLine;

            myUrl = new URL(url);

            // Create a connection
            HttpURLConnection connection = (HttpURLConnection) myUrl.openConnection();

            //Dalvik/2.1.0 (Linux; U; Android 7.1.1; ZTE A2017G Build/NMF26V)
            //System.out.println("*PROPERTY* " + System.getProperty("http.agent"));

            // Set methods and timeouts
            connection.setRequestMethod(REQUEST_METHOD);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setConnectTimeout(CONNECTION_TIMEOUT);
            // Connect to our url
            connection.connect();

            // Create a new InputStreamReader
            InputStreamReader streamReader = new InputStreamReader(connection.getInputStream(), Charset.forName("ISO-8859-1"));

            // Create a new buffered reader and String Builder
            BufferedReader reader = new BufferedReader(streamReader);
            StringBuilder stringBuilder = new StringBuilder();
            // Check if the line we are reading is not null
            while ((inputLine = reader.readLine()) != null) {
                stringBuilder.append(inputLine);
            }
            // Close our InputStream and Buffered reader
            reader.close();
            streamReader.close();
            // Set our result equal to our stringBuilder
            return stringBuilder.toString();
        }
        catch (Exception e){
            return "";
        }

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.map = googleMap;



        LatLng Turin = new LatLng(45.0477681, 7.6748638);
        map.moveCamera(CameraUpdateFactory.newLatLng(Turin));
        map.animateCamera(CameraUpdateFactory.zoomTo(13), 2000, null);
        map.setMyLocationEnabled(true);
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(final Marker marker) {

                if(!marker.getTitle().contains(" - ")){
                    return false;
                }

                final String numFermata = marker.getTitle().split(" - ")[0];
                mBottomSheetDialog.show();
                TextView tvTitle = mBottomSheetDialog.findViewById(R.id.numFermata);
                final TextView tvPassaggi = mBottomSheetDialog.findViewById(R.id.passaggi);
                tvTitle.setText(marker.getTitle());
                tvPassaggi.setText("Caricamento...");
                try{
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try  {
                                final String page =  downloadPage(passaggiUrl + numFermata).toLowerCase();
                                String textDialogTmp = "";

                                JSONArray jArray = new JSONArray(page);
                                for (int i=0; i < jArray.length(); i++)
                                {
                                    try {
                                        JSONObject objJson = jArray.getJSONObject(i);
                                        String linea = objJson.getString("linea");
                                        String direzione = objJson.getString("direzionebreve");
                                        List<String> passaggi = new ArrayList<>();
                                        JSONArray passaggiRT = objJson.getJSONArray("passaggirt");
                                        JSONArray passaggiPR = objJson.getJSONArray("passaggipr");
                                        for (int j=0; j < passaggiPR.length(); j++)
                                        {
                                            passaggi.add(passaggiPR.getString(j));

                                        }

                                        for (int j=0; j < passaggiRT.length(); j++)
                                        {
                                            passaggi.add("<b>" + passaggiRT.getString(j) + "</b>");

                                        }
                                        passaggi.sort(new Comparator<String>() {
                                            @Override
                                            public int compare(String o1, String o2) {
                                                return o1.replace("<b>","").replace("</b>","").replace("00:","88:").replace("01:","99:").compareToIgnoreCase(o2.replace("<b>","").replace("</b>","").replace("00:","88:").replace("01:","99:")) ;
                                            }
                                        });

                                        ArrayList<String> top3Passaggi;
                                        if(passaggi.size()>3){
                                            top3Passaggi = new ArrayList<String>(passaggi.subList(passaggi.size() -3, passaggi.size()));
                                        }
                                        else{
                                            top3Passaggi = new ArrayList<String>(passaggi);
                                        }

                                        top3Passaggi.sort(new Comparator<String>() {
                                            @Override
                                            public int compare(String o1, String o2) {
                                                return o1.replace("<b>","").replace("</b>","").replace("00:","88:").replace("01:","99:").compareToIgnoreCase(o2.replace("<b>","").replace("</b>","").replace("00:","88:").replace("01:","99:")) ;
                                            }
                                        });

                                        textDialogTmp+="Linea " + linea.toUpperCase()+" (" + direzione.toUpperCase() + "): " + TextUtils.join(", ", top3Passaggi) +  "<br>";
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }

                                final String textDialog = textDialogTmp;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        tvPassaggi.setText(Html.fromHtml(textDialog));
                                    }
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    thread.start();
                }
                catch (Exception e){

                    e.printStackTrace();
                }
                return false;
            }
        });
    }

    public void addMarker(BusPosition bp){

        int height = 110;
        int width = 110;
        Resources resources = getApplicationContext().getResources();
        int resourceId = resources.getIdentifier("bus_11_" + bp.getDir(), "drawable",
                getApplicationContext().getPackageName());
        if(resourceId == 0){
            height = 60;
            width = 60;
            resourceId = resources.getIdentifier("bus_00", "drawable",
                    getApplicationContext().getPackageName());
        }

        BitmapDrawable bitmapdraw = (BitmapDrawable)resources.getDrawable(resourceId);
        Bitmap b = bitmapdraw.getBitmap();
        Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);

        for(Marker m: busMarkers){
            if(m.getTitle().equals(bp.getNumMezzo() + "")){
                LatLng busPosition = new LatLng(bp.getLatitude(), bp.getLongitude());
                m.setPosition(busPosition);
                m.setIcon(BitmapDescriptorFactory.fromBitmap(smallMarker));
                return;
            }
        }
        LatLng busPosition = new LatLng(bp.getLatitude(), bp.getLongitude());
        Marker markerName = map.addMarker(new MarkerOptions().position(busPosition).title(bp.getNumMezzo() + ""));
        markerName.setIcon(BitmapDescriptorFactory.fromBitmap(smallMarker));
        busMarkers.add(markerName);
    }



    //Estrae la chiave per il download
    public String extractKeyDownload(String page){

        String key = "";
        for (String line: page.split("initGMap") ) {
            String row = line.split(";")[0];

            for(String item: row.split(",")){
                if(item.contains("\"k\"")){
                    key = item.replace("\"})", "").replace("\"k\":\"", "");
                    return key;
                }
            }

        }
        return key;
    }

    public static LatLng getCenterPosition(List<StopPosition> stopPositions)
    {

        List<LatLng> geoCoordinates = new ArrayList<>();

        for(StopPosition sp: stopPositions){
            geoCoordinates.add(new LatLng(sp.getLat(), sp.getLng()));
        }

        if (geoCoordinates.size() == 1)
        {
            return geoCoordinates.get(0);
        }

        double x = 0;
        double y = 0;
        double z = 0;

        for (LatLng geoCoordinate: geoCoordinates)
        {
            double latitude = geoCoordinate.latitude * Math.PI / 180;
            double longitude = geoCoordinate.longitude * Math.PI / 180;

            x += Math.cos(latitude) * Math.cos(longitude);
            y += Math.cos(latitude) * Math.sin(longitude);
            z += Math.sin(latitude);
        }

        double total = geoCoordinates.size();

        x = x / total;
        y = y / total;
        z = z / total;

        double centralLongitude = Math.atan2(y, x);
        double centralSquareRoot = Math.sqrt(x * x + y * y);
        double centralLatitude = Math.atan2(z, centralSquareRoot);

        return new LatLng(centralLatitude * 180 / Math.PI, centralLongitude * 180 / Math.PI);
    }




}
