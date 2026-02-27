package com.nullsec.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.nullsec.bluetooth.fragments.*
import com.nullsec.bluetooth.utils.LicenseManager

/**
 * NullSec Bluetooth - Advanced Bluetooth Security Analyzer
 * 
 * Main Activity handling navigation and permissions
 * 
 * @author @AnonAntics
 * @website https://github.com/bad-antics
 * @twitter x.com/AnonAntics
 */
class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var licenseManager: LicenseManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    
    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            initializeApp()
        } else {
            Toast.makeText(
                this,
                "🔵 Bluetooth permissions required for scanning",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (bluetoothAdapter.isEnabled) {
            initializeApp()
        } else {
            Toast.makeText(this, "Bluetooth must be enabled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        licenseManager = LicenseManager(this)
        
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        if (hasPermissions()) {
            checkBluetoothEnabled()
        } else {
            requestPermissions()
        }
    }
    
    private fun hasPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun requestPermissions() {
        permissionLauncher.launch(requiredPermissions)
    }
    
    private fun checkBluetoothEnabled() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = android.content.Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        } else {
            initializeApp()
        }
    }
    
    private fun initializeApp() {
        setupBottomNavigation()
        loadFragment(ScannerFragment())
        
        // Show premium status
        if (licenseManager.isPremium()) {
            Toast.makeText(this, "🔓 Premium features unlocked!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "🔑 Get premium at x.com/AnonAntics", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun setupBottomNavigation() {
        bottomNav = findViewById(R.id.bottom_navigation)
        
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_scanner -> {
                    loadFragment(ScannerFragment())
                    true
                }
                R.id.nav_paired -> {
                    loadFragment(PairedFragment())
                    true
                }
                R.id.nav_gatt -> {
                    if (licenseManager.isPremium()) {
                        loadFragment(GattFragment())
                    } else {
                        showPremiumRequired("GATT Explorer")
                    }
                    true
                }
                R.id.nav_tracking -> {
                    if (licenseManager.isPremium()) {
                        loadFragment(TrackingFragment())
                    } else {
                        showPremiumRequired("Device Tracking")
                    }
                    true
                }
                R.id.nav_settings -> {
                    loadFragment(SettingsFragment())
                    true
                }
                else -> false
            }
        }
    }
    
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
    
    private fun showPremiumRequired(feature: String) {
        Toast.makeText(
            this,
            "🔒 $feature requires premium!\n🔑 x.com/AnonAntics",
            Toast.LENGTH_LONG
        ).show()
    }
    
    fun isPremium(): Boolean = licenseManager.isPremium()
    
    fun getLicenseManager(): LicenseManager = licenseManager
}
