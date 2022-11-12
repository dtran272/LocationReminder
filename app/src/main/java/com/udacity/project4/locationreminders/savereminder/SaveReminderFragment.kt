package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.geofence.GeofencingConstants
import com.udacity.project4.locationreminders.geofence.GeofencingConstants.GEOFENCE_RADIUS_IN_METERS
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

const val LOCATION_PERMISSION_INDEX = 0
const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
const val REQUEST_FOREGROUND_PERMISSIONS_REQUEST_CODE = 32
const val REQUEST_BACKGROUND_AND_FOREGROUND_PERMISSION_RESULT_CODE = 33

private const val TAG = "SaveReminderFragment"

@SuppressLint("UnspecifiedImmutableFlag")
class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by sharedViewModel()
    private lateinit var binding: FragmentSaveReminderBinding

    private val runningQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)
        binding.viewModel = _viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            if (isLocationPermissionsGranted()) {
                checkDeviceLocationSettingsAndEnableGeofencing()
            } else {
                requestBackgroundLocationPermission()
            }
        }
    }

    private fun getReminderDataItem(): ReminderDataItem {
        val title = _viewModel.reminderTitle.value
        val description = _viewModel.reminderDescription.value
        val location = _viewModel.reminderSelectedLocationStr.value
        val latitude = _viewModel.latitude.value
        val longitude = _viewModel.longitude.value

        return ReminderDataItem(title, description, location, latitude, longitude)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_BACKGROUND_AND_FOREGROUND_PERMISSION_RESULT_CODE && grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED)
        ) {
            // Permission denied
            Snackbar.make(
                binding.root,
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_LONG
            ).setAction(R.string.settings) {
                startActivity(Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }.show()
        } else {
            checkDeviceLocationSettingsAndEnableGeofencing()
        }
    }

    private fun checkDeviceLocationSettingsAndEnableGeofencing(resolve: Boolean = true) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_LOW_POWER, GeofencingConstants.LOCATION_REQUEST_INTERVAL).build()

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())

        // Location permission denied
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    exception.startResolutionForResult(requireActivity(), REQUEST_TURN_DEVICE_LOCATION_ON)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.e(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                val dialog = AlertDialog.Builder(requireContext())
                    .setMessage(R.string.location_required_error)
                    .setPositiveButton(R.string.ok) { _, _ ->
                        checkDeviceLocationSettingsAndEnableGeofencing()
                    }
                    .setNegativeButton(R.string.no_thanks) { _, _ ->
                        findNavController().popBackStack()
                    }
                    .create()

                dialog.setCancelable(false)
                dialog.show()
            }
        }

        // Location permission has been approved
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                val reminderData = getReminderDataItem()

                if (_viewModel.validateEnteredData(reminderData)) {
                    buildGeofenceRequest(reminderData)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun buildGeofenceRequest(locationData: ReminderDataItem) {
        val geofencingClient = LocationServices.getGeofencingClient(requireActivity())

        val geofence = Geofence.Builder()
            .setRequestId(locationData.id)
            .setCircularRegion(
                locationData.latitude!!,
                locationData.longitude!!,
                GEOFENCE_RADIUS_IN_METERS
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
            addOnSuccessListener {
                Log.v("Add Geofence success", geofence.requestId)
                _viewModel.saveReminder(locationData)
            }

            addOnFailureListener {
                it.message?.let { message ->
                    Log.w("Add Geofence failed", message)
                }
            }
        }
    }

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this.context, GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(this.context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    @TargetApi(29)
    private fun isLocationPermissionsGranted(): Boolean {
        val isForegroundPermissionGranted =
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        val isBackgroundLocationGranted = if (runningQOrLater) {
            PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            true
        }

        return isForegroundPermissionGranted && isBackgroundLocationGranted
    }

    @TargetApi(29)
    private fun requestBackgroundLocationPermission() {
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val resultCode = when {
            runningQOrLater -> {
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION

                REQUEST_BACKGROUND_AND_FOREGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_PERMISSIONS_REQUEST_CODE
        }

        requestPermissions(
            permissionsArray,
            resultCode
        )
    }

    companion object {
        internal const val ACTION_GEOFENCE_EVENT =
            "SaveReminderFragment.locationReminder.action.ACTION_GEOFENCE_EVENT"
    }
}
