import SwiftUI

struct WatchContentView: View {
    @ObservedObject var model: WatchSessionModel

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            if let snapshot = model.snapshot {
                ActiveWatchView(snapshot: snapshot)
            } else {
                EmptyWatchView(syncState: model.syncState)
            }
        }
    }
}
