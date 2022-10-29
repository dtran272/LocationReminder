package com.udacity.project4.locationreminders

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.observe
import androidx.navigation.fragment.NavHostFragment
import com.firebase.ui.auth.AuthUI
import com.udacity.project4.R
import com.udacity.project4.authentication.AuthenticationActivity
import com.udacity.project4.authentication.AuthenticationViewModel
import com.udacity.project4.locationreminders.reminderslist.REQUEST_LOACTION_PERMISSION
import kotlinx.android.synthetic.main.activity_reminders.*
import org.koin.android.ext.android.inject

/**
 * The RemindersActivity that holds the reminders fragments
 */
class RemindersActivity : AppCompatActivity() {
    private val authViewModel: AuthenticationViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminders)

        observeAuthenticationState()
        requestLocationPermission()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                (nav_host_fragment as NavHostFragment).navController.popBackStack()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Check if the location permission are granted and if so enable the location data layer.
        if (requestCode == REQUEST_LOACTION_PERMISSION) {
            if (grantResults.isNotEmpty()) {
                when (grantResults[0]) {
                    PackageManager.PERMISSION_GRANTED -> {
                        Log.i("RemindersActivity", "Location permission is granted")
                    }
                    PackageManager.PERMISSION_DENIED -> {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                            AlertDialog.Builder(this)
                                .setTitle(getString(R.string.location_permission_alert_title))
                                .setMessage(getString(R.string.location_permission_alert_msg))
                                .setPositiveButton("Enable") { _, _ ->
                                    requestLocationPermission()
                                }.show()
                        } else {
                            AuthUI.getInstance().signOut(this)
                        }
                    }
                    else -> requestLocationPermission()
                }
            }
        }
    }

    private fun observeAuthenticationState() {
        authViewModel.authenticationState.observe(this) { authState ->
            if (authState == AuthenticationViewModel.AuthenticationState.UNAUTHENTICATED) {
                val intent = Intent(this, AuthenticationActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun isPermissionGranted(): Boolean {
        val isAccessFineLocationGranted =
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        val isAccessCoarseLocationGranted =
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        return isAccessFineLocationGranted && isAccessCoarseLocationGranted
    }

    private fun requestLocationPermission() {
        if (!isPermissionGranted()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOACTION_PERMISSION
            )
        }
    }
}
