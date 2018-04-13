package com.arashdalir.calendaralarm;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.security.Permission;
import java.util.List;

public class PermissionCheckActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_check);

        showPermissionList();

        if (AlarmCalenderHelper.checkPermissions(this))
        {
            Button b = findViewById(R.id.GrantPermissionsButton);

            if (b != null)
            {
                b.setEnabled(false);
                b.setBackgroundColor(Color.parseColor("#999999"));
                b.setTextColor(Color.parseColor("#ffffff"));
            }
        }
    }

    public void checkPermissions(View view) {
        Context context = (Context) this;
        if (!AlarmCalenderHelper.checkPermissions(context)) {
            List<String> permissionsList = AlarmCalenderHelper.getPermissions(context);
            String[] permissions = new String[permissionsList.size()];
            permissions = permissionsList.toArray(permissions);

            requestPermissions(permissions, 1);

            setContentView(R.layout.activity_permission_check);
            showPermissionList();

        } else {
            Intent intent = new Intent(context, SettingsActivity.class);
            startActivity(intent);
            finish();
        }
    }

    public void showPermissionList(){
        Context context = (Context) this;
        List<String> permissionsList = AlarmCalenderHelper.getPermissions(context);
        LinearLayout ll = findViewById(R.id.PermissionList);

        if (ll != null) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            PackageManager pm = getPackageManager();

            // Permission is not granted
            // Should we show an explanation?
            for (int i = 0; i < permissionsList.size(); i++) {
                String permission = permissionsList.get(i);
                TextView tv = new TextView(context);
                tv.setLayoutParams(params);

                if(context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED){
                //if (shouldShowRequestPermissionRationale(permission)) {
                    tv.setTextColor(Color.parseColor("#ff0000"));
                } else {
                    tv.setTextColor(Color.parseColor("#009900"));
                }

                try {
                    PermissionInfo pi = pm.getPermissionInfo(permission, 0);
                    permission = (String) pi.loadLabel(pm);
                }
                catch(Exception e)
                {
                }

                tv.setText("\u2022 " + permission);

                ll.addView(tv);
            }
        }
    }
}
