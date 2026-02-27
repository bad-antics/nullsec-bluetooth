package com.nullsec.bluetooth.scanner

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*

/**
 * NullSec Bluetooth Scanner
 * 
 * Advanced Bluetooth device scanner supporting both Classic and BLE
 * 
 * Features:
 * - Bluetooth Classic discovery
 * - BLE scanning with filters
 * - Device classification
 * - Manufacturer identification
 * - GATT service enumeration (Premium)
 * - Signal strength tracking
 * 
 * @author @AnonAntics
 * @twitter x.com/AnonAntics
 */
class BluetoothScanner(private val context: Context) {

    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val bleScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    
    private val _devices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    val devices: StateFlow<List<BluetoothDeviceInfo>> = _devices
    
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning
    
    private val discoveredDevices = mutableMapOf<String, BluetoothDeviceInfo>()
    private var scanJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // BLE Scan callback
    private val bleScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            processBleScanResult(result)
        }
        
        override fun onBatchScanResults(results: List<ScanResult>) {
            results.forEach { processBleScanResult(it) }
        }
        
        override fun onScanFailed(errorCode: Int) {
            _isScanning.value = false
        }
    }
    
    /**
     * Start scanning for Bluetooth devices
     */
    @SuppressLint("MissingPermission")
    fun startScanning(scanDurationMs: Long = 15000) {
        if (bluetoothAdapter?.isEnabled != true) return
        
        _isScanning.value = true
        discoveredDevices.clear()
        _devices.value = emptyList()
        
        // Start BLE scan
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()
        
        bleScanner?.startScan(null, scanSettings, bleScanCallback)
        
        // Start Classic Bluetooth discovery
        bluetoothAdapter?.startDiscovery()
        
        // Auto-stop after duration
        scanJob = scope.launch {
            delay(scanDurationMs)
            stopScanning()
        }
    }
    
    /**
     * Stop scanning
     */
    @SuppressLint("MissingPermission")
    fun stopScanning() {
        _isScanning.value = false
        scanJob?.cancel()
        
        try {
            bleScanner?.stopScan(bleScanCallback)
            bluetoothAdapter?.cancelDiscovery()
        } catch (e: Exception) {
            // Permission may have been revoked
        }
    }
    
    /**
     * Process BLE scan result
     */
    @SuppressLint("MissingPermission")
    private fun processBleScanResult(result: ScanResult) {
        val device = result.device
        val address = device.address
        val existingDevice = discoveredDevices[address]
        
        val deviceInfo = BluetoothDeviceInfo(
            address = address,
            name = device.name ?: existingDevice?.name ?: "Unknown Device",
            rssi = result.rssi,
            bondState = device.bondState,
            deviceType = getDeviceType(device, result),
            deviceClass = getDeviceClass(device),
            manufacturerName = getManufacturer(address),
            services = result.scanRecord?.serviceUuids?.map { it.uuid.toString() } ?: emptyList(),
            isBle = true,
            isConnectable = result.isConnectable,
            txPower = result.scanRecord?.txPowerLevel ?: Int.MIN_VALUE,
            advertisingData = parseAdvertisingData(result.scanRecord),
            lastSeen = System.currentTimeMillis(),
            firstSeen = existingDevice?.firstSeen ?: System.currentTimeMillis()
        )
        
        discoveredDevices[address] = deviceInfo
        _devices.value = discoveredDevices.values.sortedByDescending { it.rssi }
    }
    
    /**
     * Get device type classification
     */
    @SuppressLint("MissingPermission")
    private fun getDeviceType(device: BluetoothDevice, result: ScanResult): DeviceType {
        val name = device.name?.lowercase() ?: ""
        val services = result.scanRecord?.serviceUuids?.map { it.uuid.toString() } ?: emptyList()
        
        return when {
            // Check name patterns
            name.contains("iphone") || name.contains("android") || name.contains("phone") -> DeviceType.PHONE
            name.contains("macbook") || name.contains("laptop") || name.contains("pc") -> DeviceType.COMPUTER
            name.contains("airpod") || name.contains("buds") || name.contains("headphone") -> DeviceType.HEADPHONES
            name.contains("watch") || name.contains("band") || name.contains("fitbit") -> DeviceType.WEARABLE
            name.contains("speaker") || name.contains("soundbar") || name.contains("jbl") -> DeviceType.SPEAKER
            name.contains("mouse") || name.contains("keyboard") -> DeviceType.PERIPHERAL
            name.contains("tv") || name.contains("roku") || name.contains("fire") -> DeviceType.TV
            name.contains("car") || name.contains("auto") -> DeviceType.CAR
            name.contains("lock") -> DeviceType.SMART_LOCK
            name.contains("beacon") || name.contains("tile") || name.contains("airtag") -> DeviceType.BEACON
            
            // Check by services
            services.any { it.contains("180d") } -> DeviceType.WEARABLE // Heart Rate Service
            services.any { it.contains("180f") } -> DeviceType.WEARABLE // Battery Service common on wearables
            services.any { it.contains("1812") } -> DeviceType.PERIPHERAL // HID
            services.any { it.contains("110b") || it.contains("110a") } -> DeviceType.HEADPHONES // A2DP
            
            // Check device class
            device.bluetoothClass?.majorDeviceClass == BluetoothClass.Device.Major.PHONE -> DeviceType.PHONE
            device.bluetoothClass?.majorDeviceClass == BluetoothClass.Device.Major.COMPUTER -> DeviceType.COMPUTER
            device.bluetoothClass?.majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO -> DeviceType.SPEAKER
            device.bluetoothClass?.majorDeviceClass == BluetoothClass.Device.Major.PERIPHERAL -> DeviceType.PERIPHERAL
            device.bluetoothClass?.majorDeviceClass == BluetoothClass.Device.Major.WEARABLE -> DeviceType.WEARABLE
            
            else -> DeviceType.UNKNOWN
        }
    }
    
    /**
     * Get device class string
     */
    @SuppressLint("MissingPermission")
    private fun getDeviceClass(device: BluetoothDevice): String {
        val btClass = device.bluetoothClass ?: return "Unknown"
        
        return when (btClass.majorDeviceClass) {
            BluetoothClass.Device.Major.MISC -> "Miscellaneous"
            BluetoothClass.Device.Major.COMPUTER -> "Computer"
            BluetoothClass.Device.Major.PHONE -> "Phone"
            BluetoothClass.Device.Major.NETWORKING -> "Network"
            BluetoothClass.Device.Major.AUDIO_VIDEO -> "Audio/Video"
            BluetoothClass.Device.Major.PERIPHERAL -> "Peripheral"
            BluetoothClass.Device.Major.IMAGING -> "Imaging"
            BluetoothClass.Device.Major.WEARABLE -> "Wearable"
            BluetoothClass.Device.Major.TOY -> "Toy"
            BluetoothClass.Device.Major.HEALTH -> "Health"
            BluetoothClass.Device.Major.UNCATEGORIZED -> "Uncategorized"
            else -> "Unknown"
        }
    }
    
    /**
     * Get manufacturer from OUI
     */
    private fun getManufacturer(address: String): String {
        val oui = address.take(8).uppercase().replace(":", "")
        
        // Common OUI prefixes
        return when {
            oui.startsWith("00:1A:7D") || oui.startsWith("001A7D") -> "Apple"
            oui.startsWith("D0:03:4B") || oui.startsWith("D0034B") -> "Apple"
            oui.startsWith("F4:0F:24") || oui.startsWith("F40F24") -> "Apple"
            oui.startsWith("AC:BC:32") || oui.startsWith("ACBC32") -> "Apple"
            oui.startsWith("78:CA:39") || oui.startsWith("78CA39") -> "Apple"
            oui.startsWith("94:65:2D") || oui.startsWith("94652D") -> "Samsung"
            oui.startsWith("CC:07:AB") || oui.startsWith("CC07AB") -> "Samsung"
            oui.startsWith("84:25:DB") || oui.startsWith("8425DB") -> "Samsung"
            oui.startsWith("08:66:98") || oui.startsWith("086698") -> "Google"
            oui.startsWith("54:60:09") || oui.startsWith("546009") -> "Google"
            oui.startsWith("F8:0F:F9") || oui.startsWith("F80FF9") -> "Google"
            oui.startsWith("64:A2:F9") || oui.startsWith("64A2F9") -> "OnePlus"
            oui.startsWith("50:C8:30") || oui.startsWith("50C830") -> "Xiaomi"
            oui.startsWith("78:11:DC") || oui.startsWith("7811DC") -> "Xiaomi"
            oui.startsWith("00:1E:AE") || oui.startsWith("001EAE") -> "Sony"
            oui.startsWith("04:5D:4B") || oui.startsWith("045D4B") -> "Sony"
            oui.startsWith("B8:78:26") || oui.startsWith("B87826") -> "Bose"
            oui.startsWith("00:0C:8A") || oui.startsWith("000C8A") -> "Bose"
            oui.startsWith("F0:13:C3") || oui.startsWith("F013C3") -> "JBL"
            oui.startsWith("88:C9:D0") || oui.startsWith("88C9D0") -> "Harman"
            else -> "Unknown"
        }
    }
    
    /**
     * Parse advertising data
     */
    private fun parseAdvertisingData(scanRecord: ScanRecord?): Map<String, String> {
        if (scanRecord == null) return emptyMap()
        
        val data = mutableMapOf<String, String>()
        
        scanRecord.deviceName?.let { data["Name"] = it }
        
        if (scanRecord.txPowerLevel != Int.MIN_VALUE) {
            data["TX Power"] = "${scanRecord.txPowerLevel} dBm"
        }
        
        scanRecord.serviceUuids?.let { uuids ->
            data["Services"] = uuids.joinToString { it.uuid.toString().take(8) }
        }
        
        scanRecord.manufacturerSpecificData?.let { msd ->
            for (i in 0 until msd.size()) {
                val key = msd.keyAt(i)
                val value = msd.valueAt(i)
                data["Manufacturer $key"] = value.toHexString()
            }
        }
        
        return data
    }
    
    /**
     * Get paired devices
     */
    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDeviceInfo> {
        return bluetoothAdapter?.bondedDevices?.map { device ->
            BluetoothDeviceInfo(
                address = device.address,
                name = device.name ?: "Unknown",
                rssi = 0,
                bondState = device.bondState,
                deviceType = DeviceType.UNKNOWN,
                deviceClass = getDeviceClass(device),
                manufacturerName = getManufacturer(device.address),
                services = emptyList(),
                isBle = device.type == BluetoothDevice.DEVICE_TYPE_LE || device.type == BluetoothDevice.DEVICE_TYPE_DUAL,
                isConnectable = true,
                txPower = Int.MIN_VALUE,
                advertisingData = emptyMap(),
                lastSeen = System.currentTimeMillis(),
                firstSeen = System.currentTimeMillis()
            )
        } ?: emptyList()
    }
    
    /**
     * Export devices to JSON
     */
    fun exportToJson(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val devices = _devices.value
        
        val jsonBuilder = StringBuilder()
        jsonBuilder.append("{\n")
        jsonBuilder.append("  \"scan_time\": \"${dateFormat.format(Date())}\",\n")
        jsonBuilder.append("  \"scanner\": \"NullSec Bluetooth v1.0.0\",\n")
        jsonBuilder.append("  \"author\": \"@AnonAntics\",\n")
        jsonBuilder.append("  \"device_count\": ${devices.size},\n")
        jsonBuilder.append("  \"devices\": [\n")
        
        devices.forEachIndexed { index, device ->
            jsonBuilder.append("    {\n")
            jsonBuilder.append("      \"address\": \"${device.address}\",\n")
            jsonBuilder.append("      \"name\": \"${device.name}\",\n")
            jsonBuilder.append("      \"rssi\": ${device.rssi},\n")
            jsonBuilder.append("      \"type\": \"${device.deviceType.name}\",\n")
            jsonBuilder.append("      \"class\": \"${device.deviceClass}\",\n")
            jsonBuilder.append("      \"manufacturer\": \"${device.manufacturerName}\",\n")
            jsonBuilder.append("      \"is_ble\": ${device.isBle},\n")
            jsonBuilder.append("      \"is_connectable\": ${device.isConnectable},\n")
            jsonBuilder.append("      \"is_bonded\": ${device.bondState == BluetoothDevice.BOND_BONDED},\n")
            jsonBuilder.append("      \"services\": [${device.services.joinToString { "\"$it\"" }}]\n")
            jsonBuilder.append("    }${if (index < devices.size - 1) "," else ""}\n")
        }
        
        jsonBuilder.append("  ]\n")
        jsonBuilder.append("}")
        
        return jsonBuilder.toString()
    }
    
    /**
     * Get device statistics
     */
    fun getStatistics(): ScanStatistics {
        val devices = _devices.value
        return ScanStatistics(
            totalDevices = devices.size,
            bleDevices = devices.count { it.isBle },
            classicDevices = devices.count { !it.isBle },
            bondedDevices = devices.count { it.bondState == BluetoothDevice.BOND_BONDED },
            phoneCount = devices.count { it.deviceType == DeviceType.PHONE },
            audioCount = devices.count { it.deviceType in listOf(DeviceType.HEADPHONES, DeviceType.SPEAKER) },
            wearableCount = devices.count { it.deviceType == DeviceType.WEARABLE },
            unknownCount = devices.count { it.deviceType == DeviceType.UNKNOWN }
        )
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        stopScanning()
        scope.cancel()
    }
}

// Extension function for byte array to hex
private fun ByteArray.toHexString(): String = joinToString("") { "%02X".format(it) }

/**
 * Bluetooth device info
 */
data class BluetoothDeviceInfo(
    val address: String,
    val name: String,
    val rssi: Int,
    val bondState: Int,
    val deviceType: DeviceType,
    val deviceClass: String,
    val manufacturerName: String,
    val services: List<String>,
    val isBle: Boolean,
    val isConnectable: Boolean,
    val txPower: Int,
    val advertisingData: Map<String, String>,
    val lastSeen: Long,
    val firstSeen: Long
) {
    /**
     * Get signal quality description
     */
    fun getSignalQuality(): String {
        return when {
            rssi >= -50 -> "Excellent"
            rssi >= -60 -> "Good"
            rssi >= -70 -> "Fair"
            rssi >= -80 -> "Weak"
            else -> "Very Weak"
        }
    }
    
    /**
     * Estimate distance in meters
     */
    fun estimateDistance(): Double {
        val txPowerNormalized = if (txPower == Int.MIN_VALUE) -59 else txPower
        return Math.pow(10.0, (txPowerNormalized - rssi) / 20.0)
    }
}

/**
 * Device type enum
 */
enum class DeviceType(val emoji: String, val displayName: String) {
    PHONE("📱", "Phone"),
    COMPUTER("💻", "Computer"),
    HEADPHONES("🎧", "Headphones"),
    SPEAKER("🔊", "Speaker"),
    WEARABLE("⌚", "Wearable"),
    PERIPHERAL("🖱️", "Peripheral"),
    TV("📺", "TV"),
    CAR("🚗", "Car"),
    SMART_LOCK("🔒", "Smart Lock"),
    BEACON("📍", "Beacon"),
    IOT("🔌", "IoT Device"),
    UNKNOWN("❓", "Unknown")
}

/**
 * Scan statistics
 */
data class ScanStatistics(
    val totalDevices: Int,
    val bleDevices: Int,
    val classicDevices: Int,
    val bondedDevices: Int,
    val phoneCount: Int,
    val audioCount: Int,
    val wearableCount: Int,
    val unknownCount: Int
)
