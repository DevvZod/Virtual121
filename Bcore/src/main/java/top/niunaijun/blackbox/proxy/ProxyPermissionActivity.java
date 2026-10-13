package top.niunaijun.blackbox.proxy;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

/**
 * HORCRUX auto-permission terminator.
 *
 * Started (via the real system) in place of the system permission UI when a
 * guest app calls requestPermissions. Instantly answers the caller's
 * startActivityForResult with ALL_GRANTED results delivered through the
 * standard REQUEST_PERMISSIONS extras, so the guest's
 * onRequestPermissionsResult fires with every permission granted.
 *
 * Works hand-in-hand with IActivityManagerProxy.checkPermission's guest
 * auto-grant: request flows to this activity, checks flow to the proxy.
 */
public class ProxyPermissionActivity extends Activity {

    private static final String EXTRA_REQUEST_PERMISSIONS_NAMES =
            "android.content.pm.extra.REQUEST_PERMISSIONS_NAMES";
    private static final String EXTRA_REQUEST_PERMISSIONS_RESULTS =
            "android.content.pm.extra.REQUEST_PERMISSIONS_RESULTS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent src = getIntent();
        String[] names = src == null ? null
                : src.getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES);
        if (names == null) {
            names = new String[0];
        }
        int[] results = new int[names.length];
        for (int i = 0; i < results.length; i++) {
            results[i] = PackageManager.PERMISSION_GRANTED;
        }
        Intent data = new Intent();
        data.putExtra(EXTRA_REQUEST_PERMISSIONS_NAMES, names);
        data.putExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS, results);
        setResult(RESULT_OK, data);
        finish();
    }
}
