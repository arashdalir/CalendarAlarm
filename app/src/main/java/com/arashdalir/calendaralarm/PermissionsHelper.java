package com.arashdalir.calendaralarm;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.List;

class PermissionsHelper {
    public static List<String> getPermissions(Context context){
        List<String> permissions = new ArrayList<String>();
        PackageManager pm = context.getPackageManager();

        try{
            PackageInfo pi = pm.getPackageInfo(context.getApplicationContext().getPackageName(), PackageManager.GET_PERMISSIONS);

            if (pi != null && pi.requestedPermissions.length > 0)
            {
                for(int i = 0; i < pi.requestedPermissions.length; i++)
                {
                    permissions.add(pi.requestedPermissions[i]);
                }
            }
        }
        catch(Exception e)
        {

        }

        return permissions;
    }

    public static boolean checkPermissions(Context context) {
       List<String> permissions = getPermissions(context);
        for (int i = 0; i < permissions.size(); i++) {
            if (context.checkSelfPermission(permissions.get(i)) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}
