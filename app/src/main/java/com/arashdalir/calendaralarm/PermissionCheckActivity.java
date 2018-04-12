package com.arashdalir.calendaralarm;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import java.util.List;

public class PermissionCheckActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_check);
    }

    public void checkPermissions(View view) {
        Context context = (Context) this;
        if (!AlarmCalenderHelper.checkPermissions(context)) {
            List<String> permissionsList = AlarmCalenderHelper.getPermissions(context);

            // Permission is not granted
            // Should we show an explanation?
            boolean request = true;
            for (int i = 0; i < permissionsList.size(); i++) {
                if (shouldShowRequestPermissionRationale(permissionsList.get(i))) {
                    request = false;
                }
                else
                {
                    String[] permissions = new String[permissionsList.size()];
                    permissions = permissionsList.toArray(permissions);
                    requestPermissions(permissions, 1);
                }
            }
            setContentView(R.layout.activity_permission_check);
        }
    }
}
