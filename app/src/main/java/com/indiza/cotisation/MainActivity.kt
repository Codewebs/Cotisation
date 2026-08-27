package com.indiza.cotisation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface

import com.indiza.cotisation.ui.theme.CotisationTheme
import com.indiza.cotisation.sync.ConnectivityObserver
import dagger.hilt.android.AndroidEntryPoint

import javax.inject.Inject

@AndroidEntryPoint // Nécessaire si tu utilises Hilt
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var connectivityObserver: ConnectivityObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CotisationTheme {
                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppEntryPoint(connectivityObserver = connectivityObserver)
                }
            }
        }
    }
}



