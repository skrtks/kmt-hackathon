import SwiftUI

struct EmptyWatchView: View {
    let syncState: WatchSyncState

    var body: some View {
        let copy = emptyCopy
        GeometryReader { geometry in
            ScrollView {
                VStack(spacing: 8) {
                    Image(systemName: iconName)
                        .font(.system(size: 28, weight: .semibold))
                        .foregroundStyle(Color(red: 0.49, green: 0.83, blue: 1.0))
                        .padding(.bottom, 4)
                    Text(copy.title)
                        .font(.system(.title3, design: .rounded).weight(.bold))
                        .foregroundStyle(.white)
                        .multilineTextAlignment(.center)
                        .minimumScaleFactor(0.74)
                    Text(copy.detail)
                        .font(.system(.footnote, design: .rounded))
                        .foregroundStyle(Color.white.opacity(0.74))
                        .multilineTextAlignment(.center)
                        .lineLimit(3)
                        .minimumScaleFactor(0.72)
                }
                .frame(maxWidth: .infinity)
                .frame(minHeight: geometry.size.height)
                .padding(.horizontal, 18)
            }
        }
    }

    private var emptyCopy: (title: String, detail: String) {
        switch syncState {
        case .loading:
            return ("Syncing", "Checking your iPhone")
        case .noActiveWatch:
            return ("No active watch", "Start a commute on your iPhone")
        case .phoneUnavailable:
            return ("Phone unavailable", "Open Leave on your iPhone")
        case .active:
            return ("No active watch", "Start a commute on your iPhone")
        }
    }

    private var iconName: String {
        switch syncState {
        case .phoneUnavailable:
            return "iphone.slash"
        default:
            return "tram.fill"
        }
    }
}
