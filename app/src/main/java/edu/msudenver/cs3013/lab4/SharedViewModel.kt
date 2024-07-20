package edu.msudenver.cs3013.lab4

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {

    private val _parkingLocation = MutableLiveData<String>()
    val parkingLocation: LiveData<String> get() = _parkingLocation

    fun setParkingLocation(location: String) {
        _parkingLocation.value = location
    }
}

