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
            LockScreenView(state: context.state, attributes: context.attributes)
                .padding(12)
                .activityBackgroundTint(Color.black.opacity(0.85))
                .activitySystemActionForegroundColor(.white)
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
                    CountdownLabel(finalCallDate: context.state.finalCallDate)
                        .font(.headline.monospacedDigit())
                }
                DynamicIslandExpandedRegion(.center) {
                    Text(context.state.statusHeadline)
                        .font(.subheadline.weight(.semibold))
                }
                DynamicIslandExpandedRegion(.bottom) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(context.state.routeText)
                            .font(.caption)
                            .lineLimit(1)
                        LeaveWindowProgressView(state: context.state)
                    }
                }
            } compactLeading: {
                Text(context.state.lineLabel)
                    .font(.caption2.weight(.semibold))
            } compactTrailing: {
                countdown(to: context.state.finalCallDate)
                    .font(.caption2.monospacedDigit())
            } minimal: {
                Image(systemName: "tram.fill")
            }
        }
    }

    private func countdown(to date: Date) -> Text {
        Text(timerInterval: Date()...date, countsDown: true)
    }
}

@available(iOS 16.2, *)
struct LockScreenView: View {
    let state: TransitWatchContentState
    let attributes: TransitWatchAttributes

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(state.statusHeadline)
                    .font(.headline)
                    .foregroundStyle(.white)
                Spacer()
                CountdownLabel(finalCallDate: state.finalCallDate)
                    .font(.title3.monospacedDigit())
                    .foregroundStyle(.white)
            }

            Text(state.routeText)
                .font(.subheadline)
                .foregroundStyle(.white.opacity(0.85))
                .lineLimit(1)

            LeaveWindowProgressView(state: state)

            HStack(spacing: 8) {
                Label(attributes.stopName, systemImage: "mappin.and.ellipse")
                Spacer()
                Label("Leave by \(state.finalCallTimeText)", systemImage: "figure.walk")
            }
            .font(.caption)
            .foregroundStyle(.white.opacity(0.75))
        }
    }
}

@available(iOS 16.2, *)
private struct CountdownLabel: View {
    let finalCallDate: Date

    var body: some View {
        HStack(spacing: 3) {
            Text(timerInterval: Date()...finalCallDate, countsDown: true)
            Text("left")
        }
    }
}

@available(iOS 16.2, *)
private struct LeaveWindowProgressView: View {
    let state: TransitWatchContentState

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            ProgressView(timerInterval: state.windowOpenDate...state.finalCallDate, countsDown: false)
                .progressViewStyle(.linear)
                .tint(state.progressTint)
            HStack {
                Text(state.windowOpenTimeText)
                Spacer()
                Text(state.finalCallTimeText)
            }
            .font(.caption2.monospacedDigit())
            .foregroundStyle(.white.opacity(0.65))
        }
    }
}

private extension TransitWatchContentState {
    var statusHeadline: String {
        switch statusRaw {
        case "GetReady":
            return "Leave at \(windowOpenTimeText)"
        case "LeaveNow":
            return "Leave now"
        case "FinalCall":
            return "Final call"
        case "Missed":
            return "Next chance at \(windowOpenTimeText)"
        default:
            return "Leave at \(windowOpenTimeText)"
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

    var progressTint: Color {
        switch statusRaw {
        case "FinalCall":
            return Color.orange
        case "Missed":
            return Color.red
        default:
            return Color.teal
        }
    }

    private func timeText(_ date: Date) -> String {
        date.formatted(date: .omitted, time: .shortened)
    }
}
