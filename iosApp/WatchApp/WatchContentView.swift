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

private struct EmptyWatchView: View {
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

private struct ActiveWatchView: View {
    let snapshot: WatchSnapshot

    var body: some View {
        TimelineView(.periodic(from: Date(), by: 1)) { timeline in
            let nowSecondsOfDay = snapshot.currentSecondsOfDay(at: timeline.date)
            let status = snapshot.displayStatus(nowSecondsOfDay: nowSecondsOfDay)

            ZStack {
                if !snapshot.isLeaving {
                    WaterCountdownBackground(
                        fraction: snapshot.leaveWindowElapsedFraction(nowSecondsOfDay: nowSecondsOfDay)
                    )
                    FinalCallEdgeRing(isVisible: status == .finalCall)
                }
                GeometryReader { geometry in
                    ScrollView {
                        VStack(spacing: 7) {
                            Text(snapshot.headline(nowSecondsOfDay: nowSecondsOfDay))
                                .font(.system(.title2, design: .rounded).weight(.bold))
                                .foregroundStyle(.white)
                                .multilineTextAlignment(.center)
                                .lineLimit(2)
                                .minimumScaleFactor(0.62)
                                .frame(maxWidth: .infinity)

                            Text(snapshot.routeText)
                                .font(.system(.subheadline, design: .rounded).weight(.semibold))
                                .foregroundStyle(Color.white.opacity(0.90))
                                .multilineTextAlignment(.center)
                                .lineLimit(2)
                                .minimumScaleFactor(0.72)
                                .frame(maxWidth: .infinity)

                            Text(snapshot.stopName)
                                .font(.system(.footnote, design: .rounded))
                                .foregroundStyle(Color.white.opacity(0.74))
                                .lineLimit(1)
                                .minimumScaleFactor(0.7)
                                .frame(maxWidth: .infinity)

                            VStack(spacing: 3) {
                                if !snapshot.isLeaving && status != .finalCall {
                                    Text("Leave by \(WatchSnapshot.timeText(minutesOfDay: snapshot.finalCallMinutes))")
                                        .font(.system(.footnote, design: .rounded).weight(.semibold))
                                        .foregroundStyle(accentColor(for: status))
                                        .lineLimit(1)
                                        .minimumScaleFactor(0.72)
                                }
                                Text("Departure \(WatchSnapshot.timeText(minutesOfDay: snapshot.departureTimeMinutes))")
                                    .font(.system(.caption2, design: .rounded))
                                    .foregroundStyle(Color.white.opacity(0.62))
                                    .lineLimit(1)
                                    .minimumScaleFactor(0.7)
                            }
                            .padding(.top, 5)
                        }
                        .frame(maxWidth: .infinity)
                        .frame(minHeight: geometry.size.height)
                        .padding(.horizontal, 17)
                    }
                }
            }
        }
    }

    private func accentColor(for status: WatchStatus) -> Color {
        switch status {
        case .getReady:
            return Color(red: 0.49, green: 0.83, blue: 1.0)
        case .leaveNow:
            return Color(red: 0.37, green: 0.92, blue: 0.83)
        case .finalCall:
            return Color(red: 0.99, green: 0.65, blue: 0.65)
        case .missed:
            return Color(red: 0.99, green: 0.88, blue: 0.54)
        }
    }
}

private struct WaterCountdownBackground: View {
    let fraction: Double

    var body: some View {
        GeometryReader { geometry in
            let clamped = min(max(fraction, 0), 1)
            let waterHeight = geometry.size.height * (1 - clamped)
            let waterTop = geometry.size.height - waterHeight

            ZStack(alignment: .bottom) {
                Color.clear
                Rectangle()
                    .fill(Color(red: 0.01, green: 0.41, blue: 0.63))
                    .frame(height: max(waterHeight, 0))
                Rectangle()
                    .fill(Color(red: 0.22, green: 0.74, blue: 0.97))
                    .frame(height: 2)
                    .offset(y: -waterHeight + 1)
                    .opacity(waterTop < geometry.size.height ? 1 : 0)
            }
        }
        .ignoresSafeArea()
    }
}

private struct FinalCallEdgeRing: View {
    let isVisible: Bool

    var body: some View {
        if isVisible {
            GeometryReader { geometry in
                TimelineView(.animation) { timeline in
                    let phase = (timeline.date.timeIntervalSinceReferenceDate.truncatingRemainder(dividingBy: 1.8)) / 1.8
                    let pulse = phase < 0.5 ? phase * 2 : (1 - phase) * 2
                    let inset = 5 + pulse
                    let cornerRadius = min(geometry.size.width, geometry.size.height) * 0.26
                    RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                        .strokeBorder(
                            Color(red: 0.96, green: 0.25, blue: 0.36).opacity(0.58 + 0.32 * pulse),
                            lineWidth: 4 + 2 * pulse
                        )
                        .shadow(
                            color: Color(red: 0.96, green: 0.25, blue: 0.36).opacity(0.38),
                            radius: 10 + 5 * pulse
                        )
                        .padding(inset)
                }
            }
            .ignoresSafeArea()
        }
    }
}
