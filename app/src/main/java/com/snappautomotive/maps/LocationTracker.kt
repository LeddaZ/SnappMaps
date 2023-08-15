package com.snappautomotive.maps

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat
import java.util.concurrent.TimeUnit

/**
 * Responsible for initiating the tracking the users location and informing the
 * main activity when there is a change.
 *
 * This is optimised for automotive use, where power is constantly and so
 * we don't have to worry about battery related optimisations.
 */
// As a system app we know we'll get the permissions we need.
@SuppressLint("MissingPermission")
class LocationTracker(private val activity: Activity, callback: (Location) -> Unit) {

    private val locationManager = activity.getSystemService(LocationManager::class.java)
    private val listener = ListenerSimplifier(callback)

    fun startTracking() {
        when {
            ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                registerLocationListener()
            }
            shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.ACCESS_FINE_LOCATION) -> showEducationalUI()
            else -> requestPermissions()
        }
    }

    private fun showEducationalUI() {
        AlertDialog.Builder(activity)
                .setMessage(R.string.location_permission_text)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok) { _, _ -> requestPermissions() }
                .show()
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_RESULT_CODE)
    }

    fun handlePermissionResult(grantResults: IntArray) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            registerLocationListener()
        }
    }

    fun stopTracking() {
        locationManager.removeUpdates(listener)
    }

    private fun registerLocationListener() {
        val locationProvider = getLocationProvider()
        if (locationProvider == null) {
            Log.w("SMLocationTracker", "Unable to find a location provider")
            return
        }

        locationManager.requestLocationUpdates(
                locationProvider,
                TimeUnit.SECONDS.toMillis(1L), // 1s updates
                1.0F,                                    // 1 meter updates
                listener)
        locationManager.getLastKnownLocation(locationProvider)
    }

    private fun getLocationProvider(): String? {
        val locationCriteria = Criteria()
        locationCriteria.accuracy = Criteria.ACCURACY_FINE
        val provider = locationManager.getBestProvider(locationCriteria, true)
        if(provider != null) {
            return provider
        }
        locationCriteria.accuracy = Criteria.ACCURACY_COARSE
        return locationManager.getBestProvider(locationCriteria, true)
    }

    private class ListenerSimplifier(private val locationUpdateListener: (Location) -> Unit)
        : LocationListener {
        override fun onLocationChanged(location: Location) {
            locationUpdateListener(location)
        }
    }

    companion object {
        const val PERMISSION_RESULT_CODE = 1000
    }
}
