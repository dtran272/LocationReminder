package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.CancellationTokenSource
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.reminderslist.REQUEST_LOACTION_PERMISSION
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*

private const val TAG = "SelectLocationFragment"

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private var currentPoiMarker: Marker? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        requestLocationPermission()

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        return binding.root
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Check if the location permission are granted and if so enable the location data layer.
        if (requestCode == REQUEST_LOACTION_PERMISSION) {
            if (grantResults.isNotEmpty()) {
                when (grantResults[0]) {
                    PackageManager.PERMISSION_GRANTED -> {
                        Log.i("RemindersActivity", "Location permission is granted")
                        enableLocation()
                    }
                    PackageManager.PERMISSION_DENIED -> {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {
                            AlertDialog.Builder(requireContext())
                                .setTitle(getString(R.string.location_permission_alert_title))
                                .setMessage(getString(R.string.location_permission_alert_msg))
                                .setPositiveButton("Enable") { _, _ ->
                                    requestLocationPermission()
                                }.show()
                        } else {
                            findNavController().popBackStack()
                        }
                    }
                    else -> requestLocationPermission()
                }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        initMap()

        binding.saveButton.setOnClickListener {
            onLocationSelected()
        }
    }

    @SuppressLint("MissingPermission")
    private fun initMap() {
        map.isMyLocationEnabled = true
        map.uiSettings.isMapToolbarEnabled = false
        map.uiSettings.isMyLocationButtonEnabled = true

        _viewModel.selectedPOI.value?.let { selectedPoi ->
            currentPoiMarker = map.addMarker(
                MarkerOptions().position(selectedPoi.latLng).title(selectedPoi.name)
            )
        }

        zoomIntoCurrentLocation()
        setMapStyle()
        setPoiClick()
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
        fusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { location ->
                val latLng = LatLng(location.latitude, location.longitude)
                val zoomLevel = 15f

                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel))
            }
    }

    private fun onLocationSelected() {
        if (currentPoiMarker == null) {
            Toast.makeText(requireContext(), "Select point of interest before saving.", Toast.LENGTH_LONG).show()
        }

        currentPoiMarker?.let {
            _viewModel.selectedPOI.value = PointOfInterest(it.position, it.id, it.title ?: "Unknown")
            findNavController().popBackStack()
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

    private fun isPermissionGranted(): Boolean {
        val isAccessFineLocationGranted =
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        val isAccessCoarseLocationGranted =
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        return isAccessFineLocationGranted && isAccessCoarseLocationGranted
    }

    private fun requestLocationPermission() {
        if (isPermissionGranted()) {
            enableLocation()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOACTION_PERMISSION
            )
        }
    }

    private fun enableLocation() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }
}
