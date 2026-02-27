package com.nullsec.bluetooth.scanner

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/**
 * NullSec GATT Explorer (Premium Feature)
 * 
 * Connects to BLE devices and enumerates GATT services/characteristics
 * 
 * Features:
 * - Service discovery
 * - Characteristic enumeration  
 * - Property analysis (Read/Write/Notify)
 * - Descriptor listing
 * - Security assessment
 * 
 * @author @AnonAntics
 * @discord x.com/AnonAntics (Premium Required)
 */
class GattExplorer(private val context: Context) {

    private var bluetoothGatt: BluetoothGatt? = null
    
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState
    
    private val _services = MutableStateFlow<List<GattServiceInfo>>(emptyList())
    val services: StateFlow<List<GattServiceInfo>> = _services
    
    private val _explorationProgress = MutableStateFlow(0)
    val explorationProgress: StateFlow<Int> = _explorationProgress
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Known Service UUIDs
    private val knownServices = mapOf(
        "00001800-0000-1000-8000-00805f9b34fb" to "Generic Access",
        "00001801-0000-1000-8000-00805f9b34fb" to "Generic Attribute",
        "0000180a-0000-1000-8000-00805f9b34fb" to "Device Information",
        "0000180d-0000-1000-8000-00805f9b34fb" to "Heart Rate",
        "0000180f-0000-1000-8000-00805f9b34fb" to "Battery Service",
        "00001810-0000-1000-8000-00805f9b34fb" to "Blood Pressure",
        "00001812-0000-1000-8000-00805f9b34fb" to "Human Interface Device",
        "00001816-0000-1000-8000-00805f9b34fb" to "Cycling Speed and Cadence",
        "00001818-0000-1000-8000-00805f9b34fb" to "Cycling Power",
        "00001819-0000-1000-8000-00805f9b34fb" to "Location and Navigation",
        "0000181a-0000-1000-8000-00805f9b34fb" to "Environmental Sensing",
        "0000181c-0000-1000-8000-00805f9b34fb" to "User Data",
        "0000181d-0000-1000-8000-00805f9b34fb" to "Weight Scale",
        "00001822-0000-1000-8000-00805f9b34fb" to "Pulse Oximeter",
        "00001826-0000-1000-8000-00805f9b34fb" to "Fitness Machine"
    )
    
    // Known Characteristic UUIDs
    private val knownCharacteristics = mapOf(
        "00002a00-0000-1000-8000-00805f9b34fb" to "Device Name",
        "00002a01-0000-1000-8000-00805f9b34fb" to "Appearance",
        "00002a19-0000-1000-8000-00805f9b34fb" to "Battery Level",
        "00002a24-0000-1000-8000-00805f9b34fb" to "Model Number",
        "00002a25-0000-1000-8000-00805f9b34fb" to "Serial Number",
        "00002a26-0000-1000-8000-00805f9b34fb" to "Firmware Revision",
        "00002a27-0000-1000-8000-00805f9b34fb" to "Hardware Revision",
        "00002a28-0000-1000-8000-00805f9b34fb" to "Software Revision",
        "00002a29-0000-1000-8000-00805f9b34fb" to "Manufacturer Name",
        "00002a37-0000-1000-8000-00805f9b34fb" to "Heart Rate Measurement",
        "00002a38-0000-1000-8000-00805f9b34fb" to "Body Sensor Location"
    )
    
    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectionState.value = ConnectionState.CONNECTED
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectionState.value = ConnectionState.DISCONNECTED
                    bluetoothGatt?.close()
                    bluetoothGatt = null
                }
            }
        }
        
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value = ConnectionState.EXPLORING
                processServices(gatt.services)
            }
        }
        
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            // Handle characteristic read
        }
    }
    
    /**
     * Connect to device and explore GATT
     * PREMIUM FEATURE
     */
    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        _connectionState.value = ConnectionState.CONNECTING
        _services.value = emptyList()
        _explorationProgress.value = 0
        
        bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }
    
    /**
     * Disconnect from device
     */
    @SuppressLint("MissingPermission")
    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        _connectionState.value = ConnectionState.DISCONNECTED
        _services.value = emptyList()
    }
    
    /**
     * Process discovered services
     */
    private fun processServices(bluetoothServices: List<BluetoothGattService>) {
        val serviceList = mutableListOf<GattServiceInfo>()
        val totalItems = bluetoothServices.sumOf { 1 + it.characteristics.size }
        var processedItems = 0
        
        for (service in bluetoothServices) {
            val characteristics = mutableListOf<GattCharacteristicInfo>()
            
            for (char in service.characteristics) {
                val charInfo = GattCharacteristicInfo(
                    uuid = char.uuid.toString(),
                    name = knownCharacteristics[char.uuid.toString().lowercase()] ?: "Unknown",
                    properties = parseProperties(char.properties),
                    permissions = parsePermissions(char.permissions),
                    writeType = getWriteType(char.writeType),
                    descriptorCount = char.descriptors.size,
                    isReadable = (char.properties and BluetoothGattCharacteristic.PROPERTY_READ) != 0,
                    isWritable = (char.properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0,
                    hasNotify = (char.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0,
                    hasIndicate = (char.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
                )
                characteristics.add(charInfo)
                processedItems++
                _explorationProgress.value = (processedItems * 100) / totalItems
            }
            
            val serviceInfo = GattServiceInfo(
                uuid = service.uuid.toString(),
                name = knownServices[service.uuid.toString().lowercase()] ?: "Unknown Service",
                type = if (service.type == BluetoothGattService.SERVICE_TYPE_PRIMARY) "Primary" else "Secondary",
                characteristics = characteristics,
                characteristicCount = characteristics.size,
                securityLevel = assessServiceSecurity(characteristics)
            )
            serviceList.add(serviceInfo)
            processedItems++
            _explorationProgress.value = (processedItems * 100) / totalItems
        }
        
        _services.value = serviceList
        _connectionState.value = ConnectionState.EXPLORATION_COMPLETE
    }
    
    /**
     * Parse characteristic properties
     */
    private fun parseProperties(properties: Int): List<String> {
        val props = mutableListOf<String>()
        
        if ((properties and BluetoothGattCharacteristic.PROPERTY_BROADCAST) != 0) props.add("Broadcast")
        if ((properties and BluetoothGattCharacteristic.PROPERTY_READ) != 0) props.add("Read")
        if ((properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) props.add("Write No Response")
        if ((properties and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) props.add("Write")
        if ((properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) props.add("Notify")
        if ((properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) props.add("Indicate")
        if ((properties and BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE) != 0) props.add("Signed Write")
        if ((properties and BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS) != 0) props.add("Extended")
        
        return props
    }
    
    /**
     * Parse characteristic permissions
     */
    private fun parsePermissions(permissions: Int): List<String> {
        val perms = mutableListOf<String>()
        
        if ((permissions and BluetoothGattCharacteristic.PERMISSION_READ) != 0) perms.add("Read")
        if ((permissions and BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED) != 0) perms.add("Read Encrypted")
        if ((permissions and BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM) != 0) perms.add("Read Encrypted MITM")
        if ((permissions and BluetoothGattCharacteristic.PERMISSION_WRITE) != 0) perms.add("Write")
        if ((permissions and BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED) != 0) perms.add("Write Encrypted")
        if ((permissions and BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM) != 0) perms.add("Write Encrypted MITM")
        if ((permissions and BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED) != 0) perms.add("Write Signed")
        if ((permissions and BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM) != 0) perms.add("Write Signed MITM")
        
        return perms
    }
    
    /**
     * Get write type string
     */
    private fun getWriteType(writeType: Int): String {
        return when (writeType) {
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT -> "Default"
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE -> "No Response"
            BluetoothGattCharacteristic.WRITE_TYPE_SIGNED -> "Signed"
            else -> "Unknown"
        }
    }
    
    /**
     * Assess security level of service
     */
    private fun assessServiceSecurity(characteristics: List<GattCharacteristicInfo>): SecurityLevel {
        val hasEncryption = characteristics.any { 
            it.permissions.any { p -> p.contains("Encrypted") }
        }
        val hasMitm = characteristics.any {
            it.permissions.any { p -> p.contains("MITM") }
        }
        val hasOpenWrite = characteristics.any {
            it.isWritable && it.permissions.isEmpty()
        }
        
        return when {
            hasMitm -> SecurityLevel.HIGH
            hasEncryption -> SecurityLevel.MEDIUM
            hasOpenWrite -> SecurityLevel.LOW
            else -> SecurityLevel.UNKNOWN
        }
    }
    
    /**
     * Get security analysis report
     */
    fun getSecurityAnalysis(): GattSecurityAnalysis {
        val serviceList = _services.value
        val issues = mutableListOf<SecurityIssue>()
        
        serviceList.forEach { service ->
            service.characteristics.forEach { char ->
                // Check for unprotected write
                if (char.isWritable && !char.permissions.any { it.contains("Encrypted") }) {
                    issues.add(SecurityIssue(
                        severity = "HIGH",
                        characteristic = char.name,
                        issue = "Writable without encryption",
                        recommendation = "Consider requiring encrypted writes"
                    ))
                }
                
                // Check for notifications without auth
                if (char.hasNotify && !char.permissions.any { it.contains("MITM") }) {
                    issues.add(SecurityIssue(
                        severity = "MEDIUM",
                        characteristic = char.name,
                        issue = "Notifications without MITM protection",
                        recommendation = "Enable MITM protection for sensitive data"
                    ))
                }
            }
        }
        
        val overallLevel = when {
            issues.any { it.severity == "HIGH" } -> SecurityLevel.LOW
            issues.any { it.severity == "MEDIUM" } -> SecurityLevel.MEDIUM
            issues.isEmpty() -> SecurityLevel.HIGH
            else -> SecurityLevel.UNKNOWN
        }
        
        return GattSecurityAnalysis(
            overallLevel = overallLevel,
            serviceCount = serviceList.size,
            characteristicCount = serviceList.sumOf { it.characteristicCount },
            issues = issues
        )
    }
    
    /**
     * Cleanup
     */
    fun cleanup() {
        disconnect()
        scope.cancel()
    }
}

/**
 * Connection state enum
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    EXPLORING,
    EXPLORATION_COMPLETE,
    ERROR
}

/**
 * Security level enum
 */
enum class SecurityLevel(val emoji: String, val description: String) {
    HIGH("🟢", "Good security"),
    MEDIUM("🟡", "Some concerns"),
    LOW("🔴", "Security issues found"),
    UNKNOWN("⚪", "Unable to determine")
}

/**
 * GATT Service info
 */
data class GattServiceInfo(
    val uuid: String,
    val name: String,
    val type: String,
    val characteristics: List<GattCharacteristicInfo>,
    val characteristicCount: Int,
    val securityLevel: SecurityLevel
)

/**
 * GATT Characteristic info
 */
data class GattCharacteristicInfo(
    val uuid: String,
    val name: String,
    val properties: List<String>,
    val permissions: List<String>,
    val writeType: String,
    val descriptorCount: Int,
    val isReadable: Boolean,
    val isWritable: Boolean,
    val hasNotify: Boolean,
    val hasIndicate: Boolean
)

/**
 * Security issue
 */
data class SecurityIssue(
    val severity: String,
    val characteristic: String,
    val issue: String,
    val recommendation: String
)

/**
 * GATT Security analysis
 */
data class GattSecurityAnalysis(
    val overallLevel: SecurityLevel,
    val serviceCount: Int,
    val characteristicCount: Int,
    val issues: List<SecurityIssue>
)
