package com.csis4495_cmk.webuy;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class TestPageActivity extends AppCompatActivity {

    //TODO: Step 1: Declare Button
    private Button btn_groups_detail, btn_customer_homepage, btn_customer_product_detail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_page);

        //TODO: Step 2: GoToTestActivity(btnName, "btnID", testActvity.class)
        GoToTestActivity(btn_groups_detail, "btn_groups_detail", GroupsDetailActivity.class);
        GoToTestActivity(btn_customer_homepage, "btn_customer_homepage", CustomerHomePageActivity.class);
        GoToTestActivity(btn_customer_product_detail, "btn_customer_product_detail", CustomerProductDetailActivity.class);

    }

    public void GoToTestActivity(Button btnClicked, String strBtnId, Class<?> testActivity) {
        int resourceId = this.getResources().
                getIdentifier(strBtnId, "id", this.getPackageName());
        btnClicked = findViewById(resourceId);
        btnClicked.setOnClickListener(v -> {
            startActivity(new Intent(TestPageActivity.this, testActivity));
        });
    }


}