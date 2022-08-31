package org.gbsa.lecture.blesensormonitor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    private final static String TAG = "CSH_MAIN";
    private final static int PERMISSION_REQUEST_BLUETOOTH = 0;

    private View mLayout;
    ArrayList mPermissionList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLayout = findViewById(R.id.main_layout);

        findViewById(R.id.btn_scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startScanActivity();
            }
        });
    }

    private void startScanActivity() {
        /*mPermissionList = new ArrayList<>();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mPermissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);

            Intent intent = new Intent(MainActivity.this, ScanActivity.class);
            startActivity(intent);
        } else {
            requestBluetoothPermissions();
        }*/
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // Permission is already available, start camera preview

            Intent intent = new Intent(MainActivity.this, ScanActivity.class);
            startActivity(intent);
        } else {
            // Permission is missing and must be requested.
            requestBluetoothPermissions();;
        }
    }

    private void requestBluetoothPermissions() {
//        ActivityCompat.requestPermissions(this, (String[]) mPermissionList.toArray(new String[mPermissionList.size()]), PERMISSION_REQUEST_BLUETOOTH);
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // Display a SnackBar with cda button to request the missing permission.
            Snackbar.make(mLayout, R.string.bluetooth_access_required,
                    Snackbar.LENGTH_INDEFINITE).setAction(R.string.ok, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Request the permission
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            PERMISSION_REQUEST_BLUETOOTH);
                }
            }).show();

        } else {
            Snackbar.make(mLayout, R.string.bluetooth_unavailable, Snackbar.LENGTH_SHORT).show();
            // Request the permission. The result will be received in onRequestPermissionResult().
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_BLUETOOTH);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_BLUETOOTH:
                if (grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "BLUETOOTH permission OK");
                    Intent intent = new Intent(MainActivity.this, ScanActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, R.string.bluetooth_permission_denied, Toast.LENGTH_LONG)
                                    .show();
                    finish();
                }
                break;
        }

    }
}