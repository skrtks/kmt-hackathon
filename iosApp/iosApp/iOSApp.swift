import SwiftUI

@main
struct iOSApp: App {
    init() {
        if #available(iOS 16.2, *) {
            TransitLiveActivityBridge.register()
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
