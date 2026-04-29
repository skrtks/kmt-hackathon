import SwiftUI

struct ActiveWatchView: View {
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
