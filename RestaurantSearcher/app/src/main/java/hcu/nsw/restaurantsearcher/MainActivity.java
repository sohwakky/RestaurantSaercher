package hcu.nsw.restaurantsearcher;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MainActivity extends AppCompatActivity {
    //レイアウト用
    TextView tv_searchRange; //選択された検索範囲を表示
    Button bt_search; //検索ボタン
    Button bt_range300, bt_range500, bt_range1000, bt_range2000, bt_range3000; //検索範囲ボタン
    ListView lv_shopList; //店舗表示用リストビュー

    //GPS用
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private LocationManager locationManager;

    //API用
    private static final String DEBIG_TAG = "RestaurantSearcher"; //デバッグ用タグ
    private static final String HotPepper_URL =
            "http://webservice.recruit.co.jp/hotpepper/gourmet/v1/?key="; //ホットペッパーAPIのURL
    private static final String API_KEY = "57f5fb71088435f0"; //APIキー

    //店舗検索用
    int range = 3; //検索範囲（初期値：1000m）
    double lat = 34.3977; //緯度（初期値：広島駅）
    double lng = 132.4753; //経度（初期値：広島駅）

    //リスト用
    ArrayList<Map<String, Object>> listData;
    String[] names; //店舗名
    String[] accesses; //アクセス
    int[] images; //店舗画像
    String[] addresses; //住所
    String[] times; //営業時間
    String[] genres; //お店ジャンル
    String[] genre_catches; //お店ジャンルキャッチ

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv_searchRange = findViewById(R.id.tv_searchRange);
        bt_search = findViewById(R.id.bt_search);
        bt_range300 = findViewById(R.id.bt_range300);
        bt_range500 = findViewById(R.id.bt_range500);
        bt_range1000 = findViewById(R.id.bt_range1000);
        bt_range2000 = findViewById(R.id.bt_range2000);
        bt_range3000 = findViewById(R.id.bt_range3000);
        lv_shopList = findViewById(R.id.lv_shopList);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        //位置情報のパーミッション確認
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Request the permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Permission already granted, start listening for location updates
            startLocationUpdates();
        }

        //検索ボタン
        bt_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listData = new ArrayList<>();
                names = new String[100];
                accesses = new String[100];
                images = new int[100];
                addresses = new String[100];
                times = new String[100];
                genres = new String[100];
                genre_catches = new String[100];

                for(int i=0; i<100; i++){
                    images[i] = R.drawable.restaurant;
                }


                //位置情報取得
                //startLocationUpdates();
                //検索条件を加えたurlの作成
                String urlFULL = HotPepper_URL + API_KEY + "&lat=" + lat + "&lng=" + lng + "&range=" + range + "&count=100&format=json";
                //店舗情報の取得・表示
                receiveShopInfo(urlFULL);
            }
        });

        //検索範囲ボタン
        bt_range300.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tv_searchRange.setText("300m以内（徒歩4分）");
                range = 1;
            }
        });
        bt_range500.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tv_searchRange.setText("500m以内（徒歩7分）");
                range = 2;
            }
        });
        bt_range1000.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tv_searchRange.setText("1000m以内（徒歩12分）");
                range = 3;
            }
        });
        bt_range2000.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tv_searchRange.setText("2000m以内（徒歩25分）");
                range = 4;
            }
        });
        bt_range3000.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tv_searchRange.setText("3000m以内（徒歩40分）");
                range = 5;
            }
        });

        //リストタップ
        lv_shopList.setOnItemClickListener(new ListItemClickListener());
    }

    //リストタップ時の処理
    private class ListItemClickListener implements AdapterView.OnItemClickListener{
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id){
            //店舗情報を取得
            int image = images[position];
            String name = names[position];
            String address = addresses[position];
            String time = times[position];
            String genre = genres[position];
            String genre_catch = genre_catches[position];

            //インテント生成
            Intent intent = new Intent(MainActivity.this, ShopInfoActivity.class);

            //共有データを格納
            intent.putExtra("image", image);
            intent.putExtra("name", name);
            intent.putExtra("address", address);
            intent.putExtra("time", time);
            intent.putExtra("genre", genre);
            intent.putExtra("genre_catch", genre_catch);

            //画面遷移
            startActivity(intent);
        }
    }

    //////////GPS用//////////
    private void startLocationUpdates() {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, null);
        } else {
            Toast.makeText(this, "GPS is not enabled", Toast.LENGTH_SHORT).show();
        }
    }

    //現在地の格納
    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            //lat = location.getLatitude();
            //lng = location.getLongitude();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // Called when the status of the location provider changes
        }

        @Override
        public void onProviderEnabled(String provider) {
            // Called when the location provider is enabled
        }

        @Override
        public void onProviderDisabled(String provider) {
            // Called when the location provider is disabled
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start listening for location updates
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    ////////////////////

    //////////ホットペッパーAPI用//////////
    //店舗情報の取得処理
    @UiThread
    private void receiveShopInfo(final String urlFull){
        //非同期で店舗情報を取得
        ShopInfoBackgroundReceiver backgroundReceiver = new ShopInfoBackgroundReceiver(urlFull);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<String> future = executorService.submit(backgroundReceiver);
        String result = "";
        try{
            result = future.get();
        }
        catch (ExecutionException ex){
            Log.w(DEBIG_TAG, "非同期処理結果の取得で例外発生", ex);
        }
        catch (InterruptedException ex){
            Log.w(DEBIG_TAG, "非同期処理結果の取得で例外発生", ex);
        }

        showShopInfo(result);
    }

    //JSONデータの解析処理
    @UiThread
    private void showShopInfo(String result){
        try{
            JSONObject rootJSON = new JSONObject(result);
            JSONObject resultsJSON = rootJSON.getJSONObject("results");
            JSONArray shopJSONArray = resultsJSON.getJSONArray("shop");

            for(int i=0; i< shopJSONArray.length(); i++){
                //店舗情報を配列に格納
                JSONObject shopJSON = shopJSONArray.getJSONObject(i);
                names[i] = shopJSON.getString("name");
                accesses[i] = shopJSON.getString("access");
                //images[i] = shopJSON.getInt("time");
                addresses[i] = shopJSON.getString("address");
                times[i] = shopJSON.getString("open");
                JSONObject genreJSON = shopJSON.getJSONObject("genre");
                genres[i] = genreJSON.getString("name");
                genre_catches[i] = genreJSON.getString("catch");
                //リストに追加
                Map<String, Object> item = new HashMap<>();
                item.put("name", names[i]);
                item.put("access", accesses[i]);
                item.put("image", images[i]);
                listData.add(item);
            }

        }
        catch (JSONException ex){
            Log.e(DEBIG_TAG, "JSON解析失敗", ex);
        }

        lv_shopList.setAdapter(new SimpleAdapter(
                getApplicationContext(),
                listData,
                R.layout.list_item,
                new String[] {"name", "access", "image"},
                new int[] {R.id.name, R.id.access, R.id.image}
        ));
    }

    //非同期でホットペッパーAPIにアクセスするためのクラス
    private class ShopInfoBackgroundReceiver implements Callable<String> {
        private final String _urlFULL;

        public ShopInfoBackgroundReceiver(String urlFULL){
            _urlFULL = urlFULL;
        }

        @WorkerThread
        @Override
        public String call(){
            String result = "";
            //HTTP接続を行うオブジェクトを宣言
            HttpURLConnection con = null;
            //HTTPのレスポンスデータのオブジェクト宣言
            InputStream is = null;
            try{
                URL url = new URL(_urlFULL);
                con = (HttpURLConnection) url.openConnection();
                con.setConnectTimeout(1000);
                con.setReadTimeout(1000);
                con.setRequestMethod("GET");
                con.connect();
                is = con.getInputStream();
                result = is2String(is);
            }
            catch (MalformedURLException ex){
                Log.e(DEBIG_TAG, "URL変換失敗", ex);
            }
            catch (SocketTimeoutException ex){
                Log.w(DEBIG_TAG, "通信タイムアウト", ex);
            }
            catch (IOException ex){
                Log.e(DEBIG_TAG, "通信失敗", ex);
            }
            finally{
                if(con != null){
                    con.disconnect();
                }
                if(is != null){
                    try{
                        is.close();
                    }
                    catch (IOException ex){
                        Log.e(DEBIG_TAG, "InputStream解放失敗", ex);
                    }
                }
            }

            return result;
        }

        private String is2String(InputStream is) throws IOException{
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuffer sb = new StringBuffer();
            char[] b = new char[1024];
            int line;
            while(0 <= (line = reader.read(b))){
                sb.append(b, 0, line);
            }
            return sb.toString();
        }
    }
    //////////
}