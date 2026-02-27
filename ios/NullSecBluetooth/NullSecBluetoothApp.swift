import SwiftUI
import CoreBluetooth

/**
 * NullSec Bluetooth - iOS App
 * 
 * Advanced Bluetooth Security Analyzer
 * 
 * @author @AnonAntics
 * @website https://github.com/bad-antics
 * @twitter x.com/AnonAntics
 */

@main
struct NullSecBluetoothApp: App {
    @StateObject private var appState = AppState()
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(appState)
                .preferredColorScheme(.dark)
        }
    }
}

// MARK: - App State
class AppState: ObservableObject {
    @Published var isPremium: Bool = false
    @Published var devices: [BluetoothDeviceInfo] = []
    @Published var isScanning: Bool = false
    
    private let licenseManager = LicenseManager()
    
    init() {
        isPremium = licenseManager.isPremium()
    }
    
    func activateLicense(_ key: String) -> (success: Bool, message: String) {
        let result = licenseManager.activate(key: key)
        isPremium = result.success
        return result
    }
}

// MARK: - Content View
struct ContentView: View {
    @EnvironmentObject var appState: AppState
    @State private var selectedTab = 0
    
    var body: some View {
        TabView(selection: $selectedTab) {
            ScannerView()
                .tabItem {
                    Image(systemName: "antenna.radiowaves.left.and.right")
                    Text("Scanner")
                }
                .tag(0)
            
            PairedView()
                .tabItem {
                    Image(systemName: "link")
                    Text("Paired")
                }
                .tag(1)
            
            GattView()
                .tabItem {
                    Image(systemName: "list.bullet.rectangle")
                    Text("GATT")
                }
                .tag(2)
            
            SettingsView()
                .tabItem {
                    Image(systemName: "gear")
                    Text("Settings")
                }
                .tag(3)
        }
        .accentColor(NullSecColors.bluetooth)
        .onAppear {
            setupTabBarAppearance()
        }
    }
    
    private func setupTabBarAppearance() {
        let appearance = UITabBarAppearance()
        appearance.backgroundColor = UIColor(NullSecColors.surfaceDark)
        UITabBar.appearance().standardAppearance = appearance
        UITabBar.appearance().scrollEdgeAppearance = appearance
    }
}

// MARK: - Scanner View
struct ScannerView: View {
    @EnvironmentObject var appState: AppState
    @StateObject private var scanner = BluetoothScanner()
    
    var body: some View {
        NavigationView {
            ZStack {
                NullSecColors.backgroundDark.ignoresSafeArea()
                
                VStack(spacing: 0) {
                    // Stats Header
                    HStack {
                        StatBadge(value: "\(scanner.devices.count)", label: "Devices", color: NullSecColors.bluetooth)
                        Spacer()
                        StatBadge(value: "\(scanner.devices.filter { $0.isBLE }.count)", label: "BLE", color: .green)
                        Spacer()
                        StatBadge(value: "\(scanner.devices.filter { !$0.isBLE }.count)", label: "Classic", color: .orange)
                    }
                    .padding()
                    .background(NullSecColors.surfaceDark)
                    
                    // Scan Button
                    Button(action: { 
                        if scanner.isScanning {
                            scanner.stopScan()
                        } else {
                            scanner.startScan()
                        }
                    }) {
                        HStack {
                            Image(systemName: scanner.isScanning ? "stop.fill" : "antenna.radiowaves.left.and.right")
                            Text(scanner.isScanning ? "Stop Scanning" : "Start Scan")
                        }
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(scanner.isScanning ? Color.red : NullSecColors.bluetooth)
                        .cornerRadius(12)
                    }
                    .padding()
                    
                    // Device List
                    ScrollView {
                        LazyVStack(spacing: 12) {
                            ForEach(scanner.devices) { device in
                                DeviceCard(device: device)
                            }
                        }
                        .padding(.horizontal)
                    }
                }
            }
            .navigationTitle("🔵 NullSec Bluetooth")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { exportResults() }) {
                        Image(systemName: "square.and.arrow.up")
                            .foregroundColor(NullSecColors.bluetooth)
                    }
                }
            }
        }
    }
    
    private func exportResults() {
        let json = scanner.exportToJSON()
        let activityVC = UIActivityViewController(
            activityItems: [json],
            applicationActivities: nil
        )
        
        if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
           let window = windowScene.windows.first {
            window.rootViewController?.present(activityVC, animated: true)
        }
    }
}

// MARK: - Device Card
struct DeviceCard: View {
    let device: BluetoothDeviceInfo
    @State private var showDetails = false
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                // Device Type Icon
                Text(device.deviceType.emoji)
                    .font(.title2)
                
                VStack(alignment: .leading) {
                    Text(device.name)
                        .font(.headline)
                        .foregroundColor(.white)
                    
                    Text(device.identifier)
                        .font(.caption)
                        .foregroundColor(.gray)
                }
                
                Spacer()
                
                VStack(alignment: .trailing) {
                    // Signal Strength
                    HStack(spacing: 2) {
                        ForEach(0..<4) { index in
                            Rectangle()
                                .fill(index < signalBars ? signalColor : Color.gray.opacity(0.3))
                                .frame(width: 4, height: CGFloat(6 + index * 3))
                        }
                    }
                    
                    Text("\(device.rssi) dBm")
                        .font(.caption2)
                        .foregroundColor(.gray)
                }
            }
            
            if showDetails {
                Divider()
                    .background(Color.gray.opacity(0.3))
                
                HStack(spacing: 16) {
                    DetailItem(icon: "antenna.radiowaves.left.and.right", label: "Type", value: device.isBLE ? "BLE" : "Classic")
                    DetailItem(icon: "building.2", label: "Manufacturer", value: device.manufacturer)
                    if device.isBLE {
                        DetailItem(icon: "bolt.fill", label: "Connectable", value: device.isConnectable ? "Yes" : "No")
                    }
                }
                
                if !device.services.isEmpty {
                    Text("Services: \(device.services.count)")
                        .font(.caption)
                        .foregroundColor(NullSecColors.bluetooth)
                }
            }
        }
        .padding()
        .background(NullSecColors.cardBackground)
        .cornerRadius(12)
        .onTapGesture {
            withAnimation(.easeInOut(duration: 0.2)) {
                showDetails.toggle()
            }
        }
    }
    
    var signalBars: Int {
        switch device.rssi {
        case -50...0: return 4
        case -60..<(-50): return 3
        case -70..<(-60): return 2
        case -80..<(-70): return 1
        default: return 0
        }
    }
    
    var signalColor: Color {
        switch signalBars {
        case 4, 3: return .green
        case 2: return .yellow
        default: return .red
        }
    }
}

// MARK: - Paired View
struct PairedView: View {
    @StateObject private var scanner = BluetoothScanner()
    
    var body: some View {
        NavigationView {
            ZStack {
                NullSecColors.backgroundDark.ignoresSafeArea()
                
                if scanner.pairedDevices.isEmpty {
                    VStack {
                        Image(systemName: "link.badge.plus")
                            .font(.system(size: 64))
                            .foregroundColor(.gray)
                        Text("No Paired Devices")
                            .font(.headline)
                            .foregroundColor(.white)
                        Text("Pair devices in iOS Settings")
                            .font(.subheadline)
                            .foregroundColor(.gray)
                    }
                } else {
                    ScrollView {
                        LazyVStack(spacing: 12) {
                            ForEach(scanner.pairedDevices) { device in
                                DeviceCard(device: device)
                            }
                        }
                        .padding()
                    }
                }
            }
            .navigationTitle("Paired Devices")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

// MARK: - GATT View (Premium)
struct GattView: View {
    @EnvironmentObject var appState: AppState
    
    var body: some View {
        NavigationView {
            ZStack {
                NullSecColors.backgroundDark.ignoresSafeArea()
                
                if appState.isPremium {
                    VStack {
                        Image(systemName: "list.bullet.rectangle")
                            .font(.system(size: 64))
                            .foregroundColor(NullSecColors.bluetooth)
                        Text("GATT Explorer")
                            .font(.headline)
                            .foregroundColor(.white)
                        Text("Connect to a BLE device to explore services")
                            .font(.subheadline)
                            .foregroundColor(.gray)
                            .multilineTextAlignment(.center)
                    }
                    .padding()
                } else {
                    PremiumRequiredView(feature: "GATT Explorer")
                }
            }
            .navigationTitle("GATT Explorer")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

// MARK: - Settings View
struct SettingsView: View {
    @EnvironmentObject var appState: AppState
    @State private var licenseInput = ""
    @State private var showingActivation = false
    @State private var activationMessage = ""
    
    var body: some View {
        NavigationView {
            ZStack {
                NullSecColors.backgroundDark.ignoresSafeArea()
                
                Form {
                    // Premium Section
                    Section(header: Text("Premium Status")) {
                        HStack {
                            Image(systemName: appState.isPremium ? "checkmark.seal.fill" : "lock.fill")
                                .foregroundColor(appState.isPremium ? .yellow : .gray)
                            Text(appState.isPremium ? "Premium Active" : "Free Version")
                                .foregroundColor(.white)
                        }
                        
                        if !appState.isPremium {
                            Button(action: { showingActivation = true }) {
                                HStack {
                                    Image(systemName: "key.fill")
                                    Text("Enter License Key")
                                }
                                .foregroundColor(NullSecColors.bluetooth)
                            }
                            
                            Link(destination: URL(string: "https://x.com/AnonAntics")!) {
                                HStack {
                                    Image(systemName: "gift.fill")
                                    Text("Get Premium at x.com/AnonAntics")
                                }
                                .foregroundColor(.yellow)
                            }
                        }
                    }
                    
                    // Premium Features
                    Section(header: Text("Premium Features")) {
                        FeatureRow(icon: "list.bullet.rectangle", name: "GATT Explorer", isPremium: true)
                        FeatureRow(icon: "location.fill", name: "Device Tracking", isPremium: true)
                        FeatureRow(icon: "chart.line.uptrend.xyaxis", name: "Signal History", isPremium: true)
                        FeatureRow(icon: "bell.fill", name: "Proximity Alerts", isPremium: true)
                    }
                    
                    // App Info
                    Section(header: Text("About")) {
                        HStack {
                            Text("Version")
                                .foregroundColor(.gray)
                            Spacer()
                            Text("1.0.0")
                                .foregroundColor(.white)
                        }
                        
                        HStack {
                            Text("Developer")
                                .foregroundColor(.gray)
                            Spacer()
                            Text("@AnonAntics")
                                .foregroundColor(NullSecColors.bluetooth)
                        }
                        
                        Link(destination: URL(string: "https://github.com/bad-antics")!) {
                            HStack {
                                Text("GitHub")
                                    .foregroundColor(.gray)
                                Spacer()
                                Text("bad-antics")
                                    .foregroundColor(NullSecColors.bluetooth)
                                Image(systemName: "arrow.up.right.square")
                                    .foregroundColor(NullSecColors.bluetooth)
                            }
                        }
                    }
                    
                    // Disclaimer
                    Section(header: Text("Disclaimer")) {
                        Text("This app is for authorized security testing and educational purposes only. Respect privacy laws in your jurisdiction.")
                            .font(.caption)
                            .foregroundColor(.gray)
                    }
                }
                .scrollContentBackground(.hidden)
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .sheet(isPresented: $showingActivation) {
                LicenseActivationSheet(
                    licenseInput: $licenseInput,
                    message: $activationMessage,
                    onActivate: {
                        let result = appState.activateLicense(licenseInput)
                        activationMessage = result.message
                        if result.success {
                            DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                                showingActivation = false
                            }
                        }
                    }
                )
            }
        }
    }
}

// MARK: - Feature Row
struct FeatureRow: View {
    let icon: String
    let name: String
    let isPremium: Bool
    @EnvironmentObject var appState: AppState
    
    var body: some View {
        HStack {
            Image(systemName: icon)
                .foregroundColor(appState.isPremium || !isPremium ? NullSecColors.bluetooth : .gray)
            Text(name)
                .foregroundColor(.white)
            Spacer()
            if isPremium && !appState.isPremium {
                Image(systemName: "lock.fill")
                    .foregroundColor(.yellow)
            } else {
                Image(systemName: "checkmark")
                    .foregroundColor(.green)
            }
        }
    }
}

// MARK: - License Activation Sheet
struct LicenseActivationSheet: View {
    @Binding var licenseInput: String
    @Binding var message: String
    let onActivate: () -> Void
    
    var body: some View {
        NavigationView {
            ZStack {
                NullSecColors.backgroundDark.ignoresSafeArea()
                
                VStack(spacing: 24) {
                    Image(systemName: "key.fill")
                        .font(.system(size: 48))
                        .foregroundColor(NullSecColors.bluetooth)
                    
                    Text("Activate Premium")
                        .font(.title2)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                    
                    TextField("NSBT-XXXX-XXXX-XXXX-XXXX", text: $licenseInput)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                        .autocapitalization(.allCharacters)
                        .disableAutocorrection(true)
                        .padding(.horizontal)
                    
                    if !message.isEmpty {
                        Text(message)
                            .font(.caption)
                            .foregroundColor(message.contains("✓") || message.contains("🔓") ? .green : .red)
                    }
                    
                    Button(action: onActivate) {
                        Text("Activate")
                            .font(.headline)
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(NullSecColors.bluetooth)
                            .cornerRadius(12)
                    }
                    .padding(.horizontal)
                    
                    Link(destination: URL(string: "https://x.com/AnonAntics")!) {
                        Text("Get key at x.com/AnonAntics")
                            .font(.subheadline)
                            .foregroundColor(.yellow)
                    }
                    
                    Spacer()
                }
                .padding(.top, 40)
            }
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

// MARK: - Premium Required View
struct PremiumRequiredView: View {
    let feature: String
    
    var body: some View {
        VStack(spacing: 24) {
            Image(systemName: "lock.fill")
                .font(.system(size: 64))
                .foregroundColor(.yellow)
            
            Text("\(feature) requires Premium")
                .font(.headline)
                .foregroundColor(.white)
            
            Text("Unlock all features by getting a premium key")
                .font(.subheadline)
                .foregroundColor(.gray)
                .multilineTextAlignment(.center)
            
            Link(destination: URL(string: "https://x.com/AnonAntics")!) {
                HStack {
                    Image(systemName: "gift.fill")
                    Text("Get Premium")
                }
                .font(.headline)
                .foregroundColor(.black)
                .padding()
                .background(Color.yellow)
                .cornerRadius(12)
            }
        }
        .padding()
    }
}

// MARK: - Helper Views
struct StatBadge: View {
    let value: String
    let label: String
    let color: Color
    
    var body: some View {
        VStack {
            Text(value)
                .font(.title2)
                .fontWeight(.bold)
                .foregroundColor(color)
            Text(label)
                .font(.caption)
                .foregroundColor(.gray)
        }
    }
}

struct DetailItem: View {
    let icon: String
    let label: String
    let value: String
    
    var body: some View {
        VStack {
            Image(systemName: icon)
                .font(.caption)
                .foregroundColor(NullSecColors.bluetooth)
            Text(value)
                .font(.caption)
                .fontWeight(.medium)
                .foregroundColor(.white)
            Text(label)
                .font(.caption2)
                .foregroundColor(.gray)
        }
    }
}

// MARK: - Models
struct BluetoothDeviceInfo: Identifiable {
    let id = UUID()
    let identifier: String
    let name: String
    let rssi: Int
    let deviceType: DeviceType
    let manufacturer: String
    let services: [String]
    let isBLE: Bool
    let isConnectable: Bool
}

enum DeviceType {
    case phone, computer, headphones, speaker, wearable, peripheral, tv, car, smartLock, beacon, iot, unknown
    
    var emoji: String {
        switch self {
        case .phone: return "📱"
        case .computer: return "💻"
        case .headphones: return "🎧"
        case .speaker: return "🔊"
        case .wearable: return "⌚"
        case .peripheral: return "🖱️"
        case .tv: return "📺"
        case .car: return "🚗"
        case .smartLock: return "🔒"
        case .beacon: return "📍"
        case .iot: return "🔌"
        case .unknown: return "❓"
        }
    }
}

// MARK: - Colors
struct NullSecColors {
    static let bluetooth = Color(red: 0, green: 0.47, blue: 1)
    static let backgroundDark = Color(red: 0.04, green: 0.04, blue: 0.06)
    static let surfaceDark = Color(red: 0.07, green: 0.07, blue: 0.09)
    static let cardBackground = Color(red: 0.09, green: 0.09, blue: 0.12)
}

// MARK: - Bluetooth Scanner
class BluetoothScanner: NSObject, ObservableObject, CBCentralManagerDelegate {
    @Published var devices: [BluetoothDeviceInfo] = []
    @Published var pairedDevices: [BluetoothDeviceInfo] = []
    @Published var isScanning = false
    
    private var centralManager: CBCentralManager!
    private var discoveredPeripherals: [UUID: (CBPeripheral, Int)] = [:]
    
    override init() {
        super.init()
        centralManager = CBCentralManager(delegate: self, queue: nil)
    }
    
    func startScan() {
        guard centralManager.state == .poweredOn else { return }
        isScanning = true
        devices = []
        discoveredPeripherals = [:]
        centralManager.scanForPeripherals(withServices: nil, options: [CBCentralManagerScanOptionAllowDuplicatesKey: true])
    }
    
    func stopScan() {
        isScanning = false
        centralManager.stopScan()
    }
    
    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        if central.state == .poweredOn {
            // Ready to scan
        }
    }
    
    func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral, advertisementData: [String : Any], rssi RSSI: NSNumber) {
        let deviceInfo = BluetoothDeviceInfo(
            identifier: peripheral.identifier.uuidString,
            name: peripheral.name ?? advertisementData[CBAdvertisementDataLocalNameKey] as? String ?? "Unknown",
            rssi: RSSI.intValue,
            deviceType: classifyDevice(name: peripheral.name, advertisementData: advertisementData),
            manufacturer: getManufacturer(from: advertisementData),
            services: (advertisementData[CBAdvertisementDataServiceUUIDsKey] as? [CBUUID])?.map { $0.uuidString } ?? [],
            isBLE: true,
            isConnectable: advertisementData[CBAdvertisementDataIsConnectable] as? Bool ?? false
        )
        
        discoveredPeripherals[peripheral.identifier] = (peripheral, RSSI.intValue)
        
        // Update devices list
        if let index = devices.firstIndex(where: { $0.identifier == deviceInfo.identifier }) {
            devices[index] = deviceInfo
        } else {
            devices.append(deviceInfo)
        }
        
        // Sort by signal strength
        devices.sort { $0.rssi > $1.rssi }
    }
    
    private func classifyDevice(name: String?, advertisementData: [String: Any]) -> DeviceType {
        guard let name = name?.lowercased() else { return .unknown }
        
        if name.contains("phone") || name.contains("iphone") || name.contains("android") { return .phone }
        if name.contains("mac") || name.contains("laptop") || name.contains("pc") { return .computer }
        if name.contains("airpod") || name.contains("buds") || name.contains("headphone") { return .headphones }
        if name.contains("watch") || name.contains("band") || name.contains("fitbit") { return .wearable }
        if name.contains("speaker") || name.contains("jbl") || name.contains("bose") { return .speaker }
        if name.contains("mouse") || name.contains("keyboard") { return .peripheral }
        if name.contains("tv") || name.contains("roku") { return .tv }
        if name.contains("tile") || name.contains("airtag") { return .beacon }
        
        return .unknown
    }
    
    private func getManufacturer(from advertisementData: [String: Any]) -> String {
        if let manufacturerData = advertisementData[CBAdvertisementDataManufacturerDataKey] as? Data,
           manufacturerData.count >= 2 {
            let companyId = UInt16(manufacturerData[0]) | (UInt16(manufacturerData[1]) << 8)
            switch companyId {
            case 76: return "Apple"
            case 117: return "Samsung"
            case 224: return "Google"
            case 343: return "Xiaomi"
            case 301: return "Bose"
            default: return "Unknown"
            }
        }
        return "Unknown"
    }
    
    func exportToJSON() -> String {
        return """
        {
            "scanner": "NullSec Bluetooth v1.0.0",
            "author": "@AnonAntics",
            "scan_time": "\(Date())",
            "devices": \(devices.count)
        }
        """
    }
}

// MARK: - License Manager
class LicenseManager {
    private let defaults = UserDefaults.standard
    private let premiumKey = "nullsec_bluetooth_premium"
    
    func isPremium() -> Bool {
        return defaults.bool(forKey: premiumKey)
    }
    
    func activate(key: String) -> (success: Bool, message: String) {
        // Validate key format: NSBT-XXXX-XXXX-XXXX-XXXX
        let pattern = "^NSBT-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$"
        guard key.range(of: pattern, options: .regularExpression) != nil else {
            return (false, "Invalid key format")
        }
        
        defaults.set(true, forKey: premiumKey)
        return (true, "🔓 Premium activated!")
    }
}
