package com.tuapp.eventos.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.tuapp.eventos.R
import com.tuapp.eventos.databinding.ActivityMainBinding
import com.tuapp.eventos.di.SupabaseModule
import io.github.jan.supabase.auth.auth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Check for session
        val session = SupabaseModule.client.auth.currentSessionOrNull()
        if (session == null) {
            navController.navigate(R.id.loginFragment)
        }
        
        binding.bottomNavigation.setupWithNavController(navController)

        // Hide bottom navigation on detail/form screens
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.privateEventsFragment,
                R.id.publicEventsFragment,
                R.id.joinedEventsFragment -> binding.bottomNavigation.visibility = View.VISIBLE
                else -> binding.bottomNavigation.visibility = View.GONE
            }
        }
    }
}
