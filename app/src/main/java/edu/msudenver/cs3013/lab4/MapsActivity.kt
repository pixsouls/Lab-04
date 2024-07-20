package edu.msudenver.cs3013.lab4

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MapsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Set the content view before fragment transactions

        // Add the DetailFragment to the detail_fragment_container
        supportFragmentManager.beginTransaction()
            .replace(R.id.detail_fragment_container, DetailFragment())
            .commit()

        // Add the MapFragment to the map_fragment_container if it's the first time
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.map_fragment_container, MapFragment())
                .commit()
        }
    }
}
