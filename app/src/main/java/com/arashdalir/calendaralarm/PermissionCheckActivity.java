package com.arashdalir.calendaralarm;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.graphics.Typeface;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import static android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT;

public class PermissionCheckActivity extends AppCompatActivity {

    protected final int PERMISSION_REQUEST_ID = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_check);

        showPermissionList();
        toggleButton();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
        Context context = (Context) this;
        if (requestCode == PERMISSION_REQUEST_ID)
        {
            if (PermissionsHelper.checkPermissions(context)) {
                setContentView(R.layout.activity_permission_check);
                showPermissionList();

                AlarmManagerService.enqueueWork(this);
            }
        }

        toggleButton();
    }

    public void toggleButton(){
        Button b = findViewById(R.id.GrantPermissionsButton);

        if (b != null) {
            if (PermissionsHelper.checkPermissions(this)) {
                b.setEnabled(false);
                b.setText(getText(R.string.activity_permission_button_check_permissions_disabled));
            } else {
                b.setText(getText(R.string.activity_permission_button_check_permissions));
            }
        }
    }

    public void checkPermissions(View view) {
        Context context = (Context) this;
        if (!PermissionsHelper.checkPermissions(context)) {
            List<String> permissionsList = PermissionsHelper.getPermissions(context);
            String[] permissions = new String[permissionsList.size()];
            permissions = permissionsList.toArray(permissions);


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(permissions, PERMISSION_REQUEST_ID);
            }

        } else {
            Intent intent = new Intent(context, SettingsActivity.class).setFlags(FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            finish();
        }
    }

    public void showPermissionList() {
        Context context = (Context) this;
        List<String> permissionsList = PermissionsHelper.getPermissions(context);
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return OptionsItemSelectionHelper.handleOptionSelection(this, item) || super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return OptionsItemSelectionHelper.createMenuItems(this, menu);
    }
}
