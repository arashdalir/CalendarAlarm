package com.arashdalir.calendaralarm;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public class PermissionCheckActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = (Context) this;

        List<String> permissionsList = AlarmCalenderHelper.getPermissions(context);

        LinearLayout ll = findViewById(R.id.PermissionList);

        if (ll) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

            // Permission is not granted
            // Should we show an explanation?
            for (int i = 0; i < permissionsList.size(); i++) {
                String permission = permissionsList.get(i);
                TextView tv = new TextView(context);
                tv.setLayoutParams(params);
                tv.setText(permission);

                if (shouldShowRequestPermissionRationale(permission)) {
                    tv.setTextColor(Color.parseColor("#ff0000"));
                } else {
                    tv.setTextColor(Color.parseColor("#009900"));
                }
                ll.addView(tv);
            }
        }
        setContentView(R.layout.activity_permission_check);
    }

    public void checkPermissions(View view) {
        Context context = (Context) this;
        if (!AlarmCalenderHelper.checkPermissions(context)) {
            List<String> permissionsList = AlarmCalenderHelper.getPermissions(context);
            String[] permissions = new String[permissionsList.size()];
            permissions = permissionsList.toArray(permissions);

            requestPermissions(permissions, 1);

            setContentView(R.layout.activity_permission_check);
        } else {
            Intent intent = new Intent(context, SettingsActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
