package hcu.nsw.restaurantsearcher;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ShopInfoActivity extends AppCompatActivity {
    //レイアウト用
    ImageView iv_shopImage;
    TextView tv_shopName, tv_address, tv_time, tv_genre, tv_shopAddress, tv_shopTime;
    Button bt_back, bt_reserve;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop_info);

        iv_shopImage = findViewById(R.id.iv_shopImage);
        tv_shopName = findViewById(R.id.tv_shopName);
        tv_address = findViewById(R.id.tv_addess);
        tv_time = findViewById(R.id.tv_time);
        tv_shopAddress = findViewById(R.id.tv_shopAddress);
        tv_shopTime = findViewById(R.id.tv_shopTime);
        tv_genre = findViewById(R.id.tv_genre);
        bt_back = findViewById(R.id.bt_back);
        bt_reserve = findViewById(R.id.bt_reserve);

        //インテントの取得
        Intent intent = getIntent();
        //共有データの取得
        int image = intent.getIntExtra("image",0);
        String name = intent.getStringExtra("name");
        String address = intent.getStringExtra("address");
        String time = intent.getStringExtra("time");
        String genre = intent.getStringExtra("genre");
        String genre_catch = intent.getStringExtra("genre_catch");

        //店舗情報の表示
        iv_shopImage.setImageResource(image);
        tv_shopName.setText(name);
        tv_shopAddress.setText(address);
        tv_shopTime.setText(time);
        tv_genre.setText(genre);

        //戻るボタン
        bt_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }
}