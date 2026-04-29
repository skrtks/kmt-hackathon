import SwiftUI

@main
struct LeaveWindowWatchApp: App {
    @StateObject private var model = WatchSessionModel()
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            WatchContentView(model: model)
                .task {
                    model.activate()
                }
                .onChange(of: scenePhase) { _, phase in
                    model.setHapticsEnabled(phase == .active)
                }
        }
    }
}
