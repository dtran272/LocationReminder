package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.geofence.GeofencingConstants
import com.udacity.project4.locationreminders.savereminder.LOCATION_PERMISSION_INDEX
import com.udacity.project4.locationreminders.savereminder.REQUEST_FOREGROUND_PERMISSIONS_REQUEST_CODE
import com.udacity.project4.locationreminders.savereminder.REQUEST_TURN_DEVICE_LOCATION_ON
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.util.*

private const val TAG = "SelectLocationFragment"

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by sharedViewModel()
    private lateinit var binding: FragmentSelectLocationBinding

    private lateinit var map: GoogleMap

    private var currentPoiMarker: Marker? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        binding.saveButton.setOnClickListener {
            onLocationSelected()
        }

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        requestLocationPermission()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        loadMap()

        if (requestCode == REQUEST_FOREGROUND_PERMISSIONS_REQUEST_CODE &&
            (grantResults.isEmpty() || grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED)
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
            checkDeviceLocationSettingsAndMyLocation()
        }
    }

    @SuppressLint("MissingPermission")
    private fun checkDeviceLocationSettingsAndMyLocation(resolve: Boolean = true) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_LOW_POWER, GeofencingConstants.LOCATION_REQUEST_INTERVAL).build()

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())

        // Location permission denied
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    startIntentSenderForResult(exception.resolution.intentSender, REQUEST_TURN_DEVICE_LOCATION_ON, null, 0, 0, 0, null)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.e(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                val dialog = AlertDialog.Builder(requireContext())
                    .setMessage(R.string.location_required_error)
                    .setPositiveButton(R.string.ok) { _, _ ->
                        checkDeviceLocationSettingsAndMyLocation()
                    }
                    .create()

                dialog.setCancelable(false)
                dialog.show()
            }
        }

        // Location permission has been approved
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                map.isMyLocationEnabled = true
                map.uiSettings.isMyLocationButtonEnabled = true

                zoomIntoCurrentLocation()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON && resultCode == Activity.RESULT_OK) {
            checkDeviceLocationSettingsAndMyLocation(false)
        }
    }

    private fun loadMap() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        initMap()
    }

    @SuppressLint("MissingPermission")
    private fun initMap() {
        map.uiSettings.isMapToolbarEnabled = false

        _viewModel.selectedPOI.value?.let { selectedPoi ->
            currentPoiMarker = map.addMarker(
                MarkerOptions().position(selectedPoi.latLng).title(selectedPoi.name)
            )
        }

        if (currentPoiMarker == null) {
            displayInstructionToast()
        }

        setMapStyle()
        setMapClick()
        setPoiClick()
    }

    private fun setMapClick() {
        map.setOnMapClickListener { latLng ->
            currentPoiMarker?.remove()

            val snippet = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f, Long: %2$.5f",
                latLng.latitude,
                latLng.longitude
            )

            currentPoiMarker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.dropped_pin))
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))  // Changes the color of the marker
            )

            currentPoiMarker?.showInfoWindow()
        }
    }

    private fun setPoiClick() {
        map.setOnPoiClickListener { poi ->
            currentPoiMarker?.remove()

            currentPoiMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )

            currentPoiMarker?.showInfoWindow()
        }
    }

    private fun setMapStyle() {
        try {
            // Customize the styling of the base map using a JSON object defined in a raw resource file.
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_style
                )
            )

            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun zoomIntoCurrentLocation() {
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())

        fusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { location ->
                val latLng = LatLng(location.latitude, location.longitude)
                val zoomLevel = 15f

                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel))
            }
    }

    private fun onLocationSelected() {
        if (currentPoiMarker == null) {
            displayInstructionToast()
        }

        currentPoiMarker?.let {
            _viewModel.setSelectedPOI(PointOfInterest(it.position, it.id, it.title ?: "Unknown"))
            findNavController().popBackStack()
        }
    }

    private fun displayInstructionToast() {
        Toast.makeText(requireContext(), "Select point of interest before saving.", Toast.LENGTH_LONG)
            .apply {
                setGravity(Gravity.CENTER_VERTICAL, 0, 0)
                show()
            }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun isPermissionsGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        if (isPermissionsGranted()) {
            loadMap()
            checkDeviceLocationSettingsAndMyLocation()
            return
        }

        requestPermissions(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_FOREGROUND_PERMISSIONS_REQUEST_CODE
        )
    }
}
