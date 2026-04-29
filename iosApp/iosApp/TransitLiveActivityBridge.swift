import ActivityKit
import ComposeApp
import Foundation

@available(iOS 16.2, *)
final class TransitLiveActivityBridge: NSObject, LiveActivityBridge {
    static let shared = TransitLiveActivityBridge()

    private var currentActivity: Activity<TransitWatchAttributes>?

    static func register() {
        if #available(iOS 16.2, *) {
            IosLiveActivityController.shared.register(bridge: TransitLiveActivityBridge.shared)
        }
    }

    func isSupported() -> Bool {
        ActivityAuthorizationInfo().areActivitiesEnabled
    }

    func isActivityRunning() -> Bool {
        rehydrateActivity() != nil
    }

    func start(snapshot: SharedLiveActivitySnapshot) {
        guard isSupported() else { return }
        let matchingActivities = activeActivities(matchingCommuteId: snapshot.commuteId)
        if !matchingActivities.isEmpty {
            currentActivity = matchingActivities.first
            updateActivities(matchingActivities, with: snapshot)
            return
        }
        endActivities(activeActivities(), snapshot: nil, dismissalPolicy: .immediate)

        let attributes = TransitWatchAttributes(
            commuteId: snapshot.commuteId,
            stopName: snapshot.stopName
        )
        let state = makeState(snapshot)
        do {
            currentActivity = try Activity<TransitWatchAttributes>.request(
                attributes: attributes,
                content: ActivityContent(state: state, staleDate: nil),
                pushType: nil
            )
        } catch {
            currentActivity = nil
        }
    }

    func update(snapshot: SharedLiveActivitySnapshot) {
        let matchingActivities = activeActivities(matchingCommuteId: snapshot.commuteId)
        guard !matchingActivities.isEmpty else {
            start(snapshot: snapshot)
            return
        }
        currentActivity = matchingActivities.first
        updateActivities(matchingActivities, with: snapshot)
    }

    func end(snapshot: SharedLiveActivitySnapshot?, reason: SharedLiveActivityEndReason) {
        let activities = snapshot
            .map { activeActivities(matchingCommuteId: $0.commuteId) }
            ?? activeActivities()
        guard !activities.isEmpty else {
            currentActivity = nil
            return
        }
        let policy: ActivityUIDismissalPolicy = IosLiveActivityController.shared
            .isWatchStopped(reason: reason)
            ? .after(Date().addingTimeInterval(120))
            : .immediate
        endActivities(activities, snapshot: snapshot, dismissalPolicy: policy)
        currentActivity = nil
    }

    private func activeActivities(matchingCommuteId commuteId: String? = nil) -> [Activity<TransitWatchAttributes>] {
        let activities = Activity<TransitWatchAttributes>.activities
        guard let commuteId else { return activities }
        return activities.filter { $0.attributes.commuteId == commuteId }
    }

    private func rehydrateActivity(matchingCommuteId commuteId: String? = nil) -> Activity<TransitWatchAttributes>? {
        let activities = activeActivities(matchingCommuteId: commuteId)
        if let currentActivity,
           activities.contains(where: { $0.id == currentActivity.id }) {
            return currentActivity
        }

        currentActivity = activities.first
        return currentActivity
    }

    private func updateActivities(_ activities: [Activity<TransitWatchAttributes>], with snapshot: SharedLiveActivitySnapshot) {
        let state = makeState(snapshot)
        Task {
            for activity in activities {
                let alertConfiguration = makeAlertConfiguration(
                    previousState: activity.content.state,
                    nextState: state
                )
                await activity.update(
                    ActivityContent(state: state, staleDate: nil),
                    alertConfiguration: alertConfiguration
                )
            }
        }
    }

    private func makeAlertConfiguration(
        previousState: TransitWatchContentState,
        nextState: TransitWatchContentState
    ) -> AlertConfiguration? {
        guard previousState.statusRaw != nextState.statusRaw else { return nil }

        switch nextState.statusRaw {
        case "LeaveNow":
            return AlertConfiguration(
                title: "Leave now",
                body: "\(nextState.lineLabel) toward \(nextState.directionHeadsign)",
                sound: .default
            )
        case "FinalCall":
            return AlertConfiguration(
                title: "Final call",
                body: "Last chance for \(nextState.lineLabel)",
                sound: .default
            )
        default:
            return nil
        }
    }

    private func endActivities(
        _ activities: [Activity<TransitWatchAttributes>],
        snapshot: SharedLiveActivitySnapshot?,
        dismissalPolicy: ActivityUIDismissalPolicy
    ) {
        let finalState = snapshot.map(makeState)
        Task {
            for activity in activities {
                await activity.end(
                    ActivityContent(state: finalState ?? activity.content.state, staleDate: nil),
                    dismissalPolicy: dismissalPolicy
                )
            }
        }
    }

    private func makeState(_ snapshot: SharedLiveActivitySnapshot) -> TransitWatchContentState {
        let calendar = Calendar.current
        let startOfToday = calendar.startOfDay(for: Date())
        let nowOffsetSeconds = Date().timeIntervalSince(startOfToday)
        let windowOpenSeconds = TimeInterval(Int(snapshot.windowOpenMinutes) * 60)
        let finalCallSeconds = TimeInterval(Int(snapshot.finalCallMinutes) * 60)
        let departureSeconds = TimeInterval(Int(snapshot.departureTimeMinutes) * 60)
        let dayShift: TimeInterval = (finalCallSeconds < nowOffsetSeconds - 600) ? 86_400 : 0
        return TransitWatchContentState(
            title: snapshot.title,
            body: snapshot.body,
            lineLabel: snapshot.lineLabel,
            directionHeadsign: snapshot.directionHeadsign,
            walkingMinutes: Int(snapshot.walkingMinutes),
            departureDate: startOfToday.addingTimeInterval(departureSeconds + dayShift),
            windowOpenDate: startOfToday.addingTimeInterval(windowOpenSeconds + dayShift),
            finalCallDate: startOfToday.addingTimeInterval(finalCallSeconds + dayShift),
            statusRaw: IosLiveActivityController.shared.statusKey(snapshot: snapshot)
        )
    }
}
