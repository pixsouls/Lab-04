package edu.msudenver.cs3013.lab4

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import edu.msudenver.cs3013.lab4.databinding.FragmentMapsBinding

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var binding: FragmentMapsBinding? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var carMarker: Marker? = null
    private var centerMarker: Marker? = null
    private lateinit var sharedViewModel: SharedViewModel

    private val requestPermissionLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            if (fineLocationGranted || coarseLocationGranted) {
                getDeviceLocation()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMapsBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize SharedViewModel
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Set up button click listener
        binding?.btnParkedHere?.setOnClickListener {
            moveCarToCurrentLocation()
        }

        // Observe changes in parking location from SharedViewModel
        sharedViewModel.parkingLocation.observe(viewLifecycleOwner, Observer { location ->
            if (location.isNotEmpty()) {
                // Parse the location string into LatLng
                val (lat, lng) = location.split(",").map { it.toDouble() }
                val latLng = LatLng(lat, lng)
                if (carMarker == null) {
                    carMarker = mMap.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title("Parked Here")
                            .icon(BitmapDescriptorFactory.fromBitmap(vectorToBitmap(resources.getDrawable(R.drawable.baseline_directions_car_24, null))))
                    )
                } else {
                    carMarker?.position = latLng
                }
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            }
        })
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        when {
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED -> {
                mMap.isMyLocationEnabled = true
                getDeviceLocation()
            }
            else -> {
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }

        // Add a marker at the center of the map
        val initialPosition = mMap.cameraPosition.target
        centerMarker = mMap.addMarker(
            MarkerOptions()
                .position(initialPosition)
                .title("Center Position")
        )

        // Update the marker position when the map is moved
        mMap.setOnCameraMoveListener {
            val centerPosition = mMap.cameraPosition.target
            centerMarker?.position = centerPosition
        }
    }

    private fun moveCarToCurrentLocation() {
        val centerPosition = mMap.cameraPosition.target

        // Convert vector drawable to bitmap
        val carIconDrawable = resources.getDrawable(R.drawable.baseline_directions_car_24, null) ?: return
        val carIconBitmap = vectorToBitmap(carIconDrawable)

        // Move or add the car marker
        if (carMarker == null) {
            carMarker = mMap.addMarker(
                MarkerOptions()
                    .position(centerPosition)
                    .title("Parked Here")
                    .icon(BitmapDescriptorFactory.fromBitmap(carIconBitmap))
            )
        } else {
            carMarker?.position = centerPosition
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(centerPosition, 15f))

        // Update the SharedViewModel with the new location
        sharedViewModel.setParkingLocation("${centerPosition.latitude},${centerPosition.longitude}")
    }

    private fun getDeviceLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permissions are not granted, return
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener(requireActivity()) { location ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                }
            }
    }

    private fun vectorToBitmap(drawable: Drawable): Bitmap {
        var drawableVar = drawable
        if (drawableVar is VectorDrawable) {
            drawableVar = DrawableCompat.wrap(drawableVar).mutate()
        }
        val bitmap = Bitmap.createBitmap(
            drawableVar.intrinsicWidth,
            drawableVar.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawableVar.setBounds(0, 0, canvas.width, canvas.height)
        drawableVar.draw(canvas)
        return bitmap
    }
}
