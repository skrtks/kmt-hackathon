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
                    countdown(to: context.state.finalCallDate)
                        .font(.title3.monospacedDigit())
                }
                DynamicIslandExpandedRegion(.center) {
                    Text(context.state.title)
                        .font(.subheadline.weight(.semibold))
                }
                DynamicIslandExpandedRegion(.bottom) {
                    Text(context.state.body)
                        .font(.caption)
                        .lineLimit(2)
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
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(state.title)
                    .font(.headline)
                    .foregroundStyle(.white)
                Spacer()
                Text(timerInterval: Date()...state.finalCallDate, countsDown: true)
                    .font(.title3.monospacedDigit())
                    .foregroundStyle(.white)
            }
            Text(state.body)
                .font(.subheadline)
                .foregroundStyle(.white.opacity(0.85))
                .lineLimit(2)
            HStack(spacing: 8) {
                Label(attributes.stopName, systemImage: "mappin.and.ellipse")
                Spacer()
                Label("\(state.walkingMinutes) min walk", systemImage: "figure.walk")
            }
            .font(.caption)
            .foregroundStyle(.white.opacity(0.75))
        }
    }
}
