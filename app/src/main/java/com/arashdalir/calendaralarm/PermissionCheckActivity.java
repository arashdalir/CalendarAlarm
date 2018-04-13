package com.arashdalir.calendaralarm;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public class PermissionCheckActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_check);

        showPermissionList();
        Button b = findViewById(R.id.GrantPermissionsButton);

        if (b != null) {
            if (AlarmCalenderHelper.checkPermissions(this)) {
                b.setEnabled(false);
                b.setText(getText(R.string.activity_permission_button_check_permissions_disabled));
            } else {
                b.setText(getText(R.string.activity_permission_button_check_permissions));
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

    public void showPermissionList() {
        Context context = (Context) this;
        List<String> permissionsList = AlarmCalenderHelper.getPermissions(context);
        LinearLayout llg = findViewById(R.id.PermissionList_Granted);
        LinearLayout llr = findViewById(R.id.PermissionList_Rejected);

        boolean hasRejected = false,
                hasGranted = false;

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, (int) getResources().getDimension(R.dimen.spacing), 0, 0);

        if (llg != null && llr != null) {

            PackageManager pm = getPackageManager();

            // Permission is not granted
            // Should we show an explanation?
            for (int i = 0; i < permissionsList.size(); i++) {
                String permission = permissionsList.get(i);
                TextView tv = new TextView(context);
                tv.setLayoutParams(params);

                String hrPermission = permission;

                try {
                    PermissionInfo pi = pm.getPermissionInfo(permission, 0);
                    hrPermission = (String) pi.loadLabel(pm);
                } catch (Exception e) {
                }

                boolean granted = (context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);

                int colorCode = granted?R.color.colorPermissionsGranted:R.color.colorPermissionsRejected;

                int color = ContextCompat.getColor(context, colorCode);

                if (
                        (granted && !hasGranted) ||
                        (!granted && !hasRejected)
                        )
                {
                    TextView title = new TextView(context);

                    title.setTextColor(color);
                    title.setText(granted?R.string.activity_permission_granted:R.string.activity_permission_rejected);
                    title.setTypeface(null, Typeface.BOLD);

                    (granted?llg:llr).addView(title);
                }

                if (granted) {
                    hasGranted = true;
                }else
                {
                    hasRejected=true;
                }

                tv.setText("\u2022 " + hrPermission);
                tv.setTextColor(color);

                (granted?llg:llr).addView(tv);
            }
        }
    }
}
