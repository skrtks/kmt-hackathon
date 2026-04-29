import ActivityKit
import SwiftUI
import WidgetKit

@main
struct TransitLiveActivityBundle: WidgetBundle {
    var body: some Widget {
        if #available(iOS 16.2, *) {
            TransitLiveActivityWidget()
        }
    }
}

@available(iOS 16.2, *)
struct TransitLiveActivityWidget: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: TransitWatchAttributes.self) { context in
            TimelineView(.periodic(from: Date(), by: 1)) { timeline in
                LockScreenView(
                    state: context.state,
                    attributes: context.attributes,
                    now: timeline.date,
                    isStale: context.isStale
                )
                .padding(12)
                .background {
                    LiveActivityCanvasGradient(
                        intensity: context.state.leaveWindowRemainingFraction(
                            at: timeline.date,
                            isStale: context.isStale
                        ),
                        accent: context.state.progressTint(at: timeline.date, isStale: context.isStale)
                    )
                }
                .activityBackgroundTint(context.state.backgroundTint(at: timeline.date, isStale: context.isStale))
                .activitySystemActionForegroundColor(.white)
            }
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(context.state.lineLabel)
                            .font(.headline)
                        Text(context.state.directionHeadsign)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
                DynamicIslandExpandedRegion(.trailing) {
                    Text(context.state.finalCallTimeText)
                        .font(.headline.monospacedDigit())
                }
                DynamicIslandExpandedRegion(.center) {
                    TimelineView(.periodic(from: Date(), by: 1)) { timeline in
                        Text(context.state.statusHeadline(at: timeline.date, isStale: context.isStale))
                            .font(.subheadline.weight(.semibold))
                    }
                }
                DynamicIslandExpandedRegion(.bottom) {
                    TimelineView(.periodic(from: Date(), by: 1)) { timeline in
                        VStack(alignment: .leading, spacing: 6) {
                            Text(context.state.supportingText(at: timeline.date, isStale: context.isStale))
                                .font(.caption)
                                .lineLimit(1)
                            LeaveWindowTimingCaptionView(state: context.state, now: timeline.date, isStale: context.isStale)
                        }
                    }
                }
            } compactLeading: {
                Image(systemName: "tram.fill")
            } compactTrailing: {
                TimelineView(.periodic(from: Date(), by: 1)) { timeline in
                    CompactTrailingStatusView(
                        state: context.state,
                        now: timeline.date,
                        isStale: context.isStale
                    )
                }
            } minimal: {
                TimelineView(.periodic(from: Date(), by: 1)) { timeline in
                    CompactTrailingStatusView(
                        state: context.state,
                        now: timeline.date,
                        isStale: context.isStale
                    )
                }
            }
        }
    }
}

@available(iOS 16.2, *)
struct LockScreenView: View {
    let state: TransitWatchContentState
    let attributes: TransitWatchAttributes
    let now: Date
    let isStale: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(state.statusHeadline(at: now, isStale: isStale))
                    .font(.headline)
                    .foregroundStyle(.white)
                Spacer()
                Text(state.finalCallTimeText)
                    .font(.headline.monospacedDigit())
                    .foregroundStyle(.white)
            }

            Text(state.supportingText(at: now, isStale: isStale))
                .font(.subheadline)
                .foregroundStyle(.white.opacity(0.85))
                .lineLimit(1)

            LeaveWindowTimingCaptionView(state: state, now: now, isStale: isStale)

            HStack(spacing: 8) {
                Label(attributes.stopName, systemImage: "mappin.and.ellipse")
                Spacer()
                Label(state.footerText(at: now, isStale: isStale), systemImage: "figure.walk")
            }
            .font(.caption)
            .foregroundStyle(.white.opacity(0.75))
        }
    }
}

@available(iOS 16.2, *)
private struct CompactTrailingStatusView: View {
    let state: TransitWatchContentState
    let now: Date
    let isStale: Bool

    var body: some View {
        switch state.displayStatus(at: now, isStale: isStale) {
        case .getReady:
            Image(systemName: "hourglass")
        case .leaveNow:
            CompactCountdownRing(
                interval: state.windowOpenDate...state.finalCallDate,
                tint: state.progressTint(at: now, isStale: isStale)
            )
        case .finalCall:
            Image(systemName: "figure.run")
        case .expired, .stopped, .ended:
            Image(systemName: "tram.fill")
        }
    }
}

@available(iOS 16.2, *)
private struct CompactCountdownRing: View {
    let interval: ClosedRange<Date>
    let tint: Color

    var body: some View {
        ProgressView(timerInterval: interval, countsDown: true) {
            EmptyView()
        } currentValueLabel: {
            EmptyView()
        }
            .progressViewStyle(.circular)
            .tint(tint.opacity(0.95))
            .frame(width: 16, height: 16)
    }
}

@available(iOS 16.2, *)
private struct LiveActivityCanvasGradient: View {
    let intensity: Double
    let accent: Color

    var body: some View {
        let clampedIntensity = min(max(intensity, 0), 1)
        ZStack {
            LinearGradient(
                colors: [
                    .white.opacity(0.28),
                    accent.opacity(0.56),
                    accent.opacity(0.22),
                    .clear
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            LinearGradient(
                colors: [
                    .clear,
                    accent.opacity(0.30),
                    .clear
                ],
                startPoint: .leading,
                endPoint: .trailing
            )
            .blendMode(.screen)
        }
        .opacity(clampedIntensity)
    }
}

@available(iOS 16.2, *)
private struct LeaveWindowTimingCaptionView: View {
    let state: TransitWatchContentState
    let now: Date
    let isStale: Bool

    var body: some View {
        if state.displayStatus(at: now, isStale: isStale) == .getReady {
            HStack {
                Text("Starts in")
                Spacer()
                Text(timerInterval: now...(state.windowOpenDate > now ? state.windowOpenDate : now), countsDown: true)
            }
            .font(.caption2.monospacedDigit())
            .foregroundStyle(.white.opacity(0.65))
        }
    }
}

private enum TransitDisplayStatus {
    case getReady
    case leaveNow
    case finalCall
    case expired
    case stopped
    case ended
}

private extension TransitWatchContentState {
    func displayStatus(at date: Date, isStale _: Bool) -> TransitDisplayStatus {
        switch statusRaw {
        case "WatchStopped":
            return .stopped
        case "Ended":
            return .ended
        default:
            break
        }

        if date > departureDate {
            return .expired
        }
        if date < windowOpenDate {
            return .getReady
        }
        if date < finalCallDate {
            return .leaveNow
        }
        return .finalCall
    }

    func statusHeadline(at date: Date, isStale: Bool) -> String {
        switch displayStatus(at: date, isStale: isStale) {
        case .getReady:
            return "Leave at \(windowOpenTimeText)"
        case .leaveNow:
            return "Leave now"
        case .finalCall:
            return "Final call"
        case .expired:
            return "Open app for next chance"
        case .stopped, .ended:
            return title
        }
    }

    func supportingText(at date: Date, isStale: Bool) -> String {
        switch displayStatus(at: date, isStale: isStale) {
        case .expired, .stopped, .ended:
            return body
        case .getReady, .leaveNow, .finalCall:
            return routeText
        }
    }

    func footerText(at date: Date, isStale: Bool) -> String {
        if isExpired(at: date, isStale: isStale) {
            return "Open the app to refresh"
        }
        return "Leave by \(finalCallTimeText)"
    }

    func isExpired(at date: Date, isStale: Bool) -> Bool {
        displayStatus(at: date, isStale: isStale) == .expired
    }

    func leaveWindowRemainingFraction(at date: Date, isStale: Bool) -> Double {
        switch displayStatus(at: date, isStale: isStale) {
        case .getReady:
            return 1
        case .leaveNow:
            let duration = finalCallDate.timeIntervalSince(windowOpenDate)
            guard duration > 0 else { return 0 }
            return min(max(finalCallDate.timeIntervalSince(date) / duration, 0), 1)
        case .finalCall, .expired, .stopped, .ended:
            return 0
        }
    }

    var routeText: String {
        "\(lineLabel) to \(directionHeadsign)"
    }

    var windowOpenTimeText: String {
        timeText(windowOpenDate)
    }

    var finalCallTimeText: String {
        timeText(finalCallDate)
    }

    func progressTint(at date: Date, isStale: Bool) -> Color {
        switch displayStatus(at: date, isStale: isStale) {
        case .finalCall:
            return Color.orange
        case .expired, .stopped, .ended:
            return Color.red
        case .getReady, .leaveNow:
            return Color.teal
        }
    }

    func backgroundTint(at date: Date, isStale: Bool) -> Color {
        switch displayStatus(at: date, isStale: isStale) {
        case .finalCall:
            return Color.orange.opacity(0.82)
        case .expired, .stopped, .ended:
            return Color.red.opacity(0.82)
        case .getReady, .leaveNow:
            return Color.black.opacity(0.85)
        }
    }

    private func timeText(_ date: Date) -> String {
        date.formatted(date: .omitted, time: .shortened)
    }
}
