package com.udacity.project4.locationreminders

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.observe
import androidx.navigation.fragment.NavHostFragment
import com.udacity.project4.R
import com.udacity.project4.authentication.AuthenticationActivity
import com.udacity.project4.authentication.AuthenticationViewModel
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

    private fun observeAuthenticationState() {
        authViewModel.authenticationState.observe(this) { authState ->
            if (authState == AuthenticationViewModel.AuthenticationState.UNAUTHENTICATED) {
                val intent = Intent(this, AuthenticationActivity::class.java)
                startActivity(intent)
            }
        }
    }
}
