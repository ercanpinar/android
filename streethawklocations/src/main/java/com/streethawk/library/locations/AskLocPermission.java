package com.streethawk.library.locations;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.streethawk.library.core.Util;

public class AskLocPermission extends AppCompatActivity {
    Activity mActivity;
    boolean showDialog = false;
    private final String SUBTAG = "AskLocPermission ";
    private final int PERMISSIONS_LOCATION = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ask_loc_permission);
        showDialog = getIntent().getBooleanExtra(Constants.PERMISSION_BOOL,false);
    }

    public void onResume(){
        super.onResume();
        if(showDialog) {
            String msg = getIntent().getStringExtra(Constants.PERMISSION_MSG);
            displayPermissionDialog(msg);
        }
        mActivity = this;
    }

    /**
     * Check for location permission for Android MarshMallow
     * @param context
     * @return
     */
    private boolean checkForLocationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int coarseLocation = context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION);
            int fineLocation = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            if ((coarseLocation == PackageManager.PERMISSION_GRANTED) || (fineLocation == PackageManager.PERMISSION_GRANTED)) {
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    private View.OnClickListener SnackBarOnclickListener(){
        return new View.OnClickListener(){
            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                mActivity.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSIONS_LOCATION);
            }
        };
    }


    private void displayPermissionDialog(String msg){
        String buttonText = getResources().getString(R.string.sh_loc_permission);
        if(null==buttonText){
            buttonText = "Okay";
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(!checkForLocationPermission(this)) {
                if(shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)){
                    CoordinatorLayout coordinatorLayout = (CoordinatorLayout)findViewById(R.id.loccoordinatorLayout);
                    Snackbar.make(coordinatorLayout,msg, Snackbar.LENGTH_INDEFINITE)
                            .setAction(buttonText, SnackBarOnclickListener())
                            .show();
                }else{
                    this.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                            PERMISSIONS_LOCATION);
                }
            }else{
                Log.i(Util.TAG, SUBTAG + "App already has the permission");
                finish();
            }
        } else {
            Log.e(Util.TAG,SUBTAG+"Not requesting permission "+Build.VERSION.SDK_INT+" "+Build.VERSION_CODES.M);
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    SHLocation.getInstance(this).startLocationReporting();
                } else {

                    Log.e(Util.TAG, "Permission not granted by user");
                }
            }
        }
        finish();
    }

}
