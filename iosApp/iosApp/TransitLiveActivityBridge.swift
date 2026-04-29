import ActivityKit
import ComposeApp
import Foundation

@available(iOS 16.2, *)
final class TransitLiveActivityBridge: NSObject, LiveActivityBridge {
    static let shared = TransitLiveActivityBridge()

    private var currentActivity: Activity<TransitWatchAttributes>?
    private var operationTask: Task<Void, Never>?
    private var desiredStateByCommuteId: [String: TransitWatchContentState] = [:]
    private var refreshTasksByCommuteId: [String: [Task<Void, Never>]] = [:]

    static func register() {
        IosLiveActivityController.shared.register(bridge: TransitLiveActivityBridge.shared)
    }

    func isSupported() -> Bool {
        areLiveActivitiesEnabled || WatchConnectivitySnapshotBridge.shared.isSupported
    }

    func isActivityRunning() -> Bool {
        rehydrateActivity() != nil || WatchConnectivitySnapshotBridge.shared.hasActiveSnapshot
    }

    func start(snapshot: SharedLiveActivitySnapshot) -> Bool {
        let watchSynced = WatchConnectivitySnapshotBridge.shared.publish(snapshot: snapshot)
        guard areLiveActivitiesEnabled else { return watchSynced }

        let matchingActivities = activeActivities(matchingCommuteId: snapshot.commuteId)
        if !matchingActivities.isEmpty {
            currentActivity = matchingActivities.first
            return updateActivities(matchingActivities, with: snapshot)
        }

        let oldActivities = activeActivities()
        if !oldActivities.isEmpty {
            let state = makeState(snapshot)
            if desiredStateByCommuteId[snapshot.commuteId] == state { return false }
            desiredStateByCommuteId[snapshot.commuteId] = state
            enqueueOperation { [weak self, oldActivities, snapshot, state] in
                guard let self else { return }
                await self.endActivitiesNow(
                    oldActivities,
                    snapshot: nil,
                    dismissalPolicy: .immediate,
                    reason: SharedLiveActivityEndReason.sessionended
                )
                guard self.desiredStateByCommuteId[snapshot.commuteId] == state else { return }
                _ = self.requestActivity(snapshot)
            }
            return watchSynced
        }

        return requestActivity(snapshot)
    }

    func update(snapshot: SharedLiveActivitySnapshot) -> Bool {
        let watchSynced = WatchConnectivitySnapshotBridge.shared.publish(snapshot: snapshot)
        guard areLiveActivitiesEnabled else { return watchSynced }

        let matchingActivities = activeActivities(matchingCommuteId: snapshot.commuteId)
        guard !matchingActivities.isEmpty else {
            return start(snapshot: snapshot)
        }

        currentActivity = matchingActivities.first
        return updateActivities(matchingActivities, with: snapshot)
    }

    func end(snapshot: SharedLiveActivitySnapshot?, reason: SharedLiveActivityEndReason) -> Bool {
        if reason == .leaving, let snapshot {
            WatchConnectivitySnapshotBridge.shared.publish(snapshot: snapshot)
        } else {
            WatchConnectivitySnapshotBridge.shared.clear(snapshot: snapshot)
        }

        let activities = snapshot
            .map { activeActivities(matchingCommuteId: $0.commuteId) }
            ?? activeActivities()
        guard !activities.isEmpty else {
            currentActivity = nil
            return true
        }

        let policy: ActivityUIDismissalPolicy = IosLiveActivityController.shared
            .isWatchStopped(reason: reason)
            ? .after(Date().addingTimeInterval(120))
            : .immediate
        endActivities(activities, snapshot: snapshot, dismissalPolicy: policy, reason: reason)
        currentActivity = nil
        return true
    }

    private var areLiveActivitiesEnabled: Bool {
        ActivityAuthorizationInfo().areActivitiesEnabled
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

    private func requestActivity(_ snapshot: SharedLiveActivitySnapshot) -> Bool {
        let attributes = TransitWatchAttributes(
            commuteId: snapshot.commuteId,
            stopName: snapshot.stopName
        )
        let state = makeState(snapshot)
        desiredStateByCommuteId[snapshot.commuteId] = state

        do {
            currentActivity = try Activity<TransitWatchAttributes>.request(
                attributes: attributes,
                content: activityContent(state: state),
                pushType: nil
            )
            scheduleTransitionRefresh(for: snapshot.commuteId, state: state)
            return true
        } catch {
            desiredStateByCommuteId[snapshot.commuteId] = nil
            cancelTransitionRefresh(for: snapshot.commuteId)
            currentActivity = nil
            logActivityError("request", error)
            return false
        }
    }

    private func updateActivities(_ activities: [Activity<TransitWatchAttributes>], with snapshot: SharedLiveActivitySnapshot) -> Bool {
        let state = makeState(snapshot)
        desiredStateByCommuteId[snapshot.commuteId] = state
        scheduleTransitionRefresh(for: snapshot.commuteId, state: state)
        guard activities.contains(where: { $0.content.state != state }) else { return true }

        enqueueOperation { [weak self, activities, state, commuteId = snapshot.commuteId] in
            guard let self else { return }
            guard self.desiredStateByCommuteId[commuteId] == state else { return }

            for activity in activities {
                let alertConfiguration = self.makeAlertConfiguration(
                    previousState: activity.content.state,
                    nextState: state
                )
                await activity.update(
                    self.activityContent(state: state),
                    alertConfiguration: alertConfiguration
                )
            }
        }
        return true
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
        dismissalPolicy: ActivityUIDismissalPolicy,
        reason: SharedLiveActivityEndReason
    ) {
        enqueueOperation { [weak self, activities, snapshot, dismissalPolicy, reason] in
            guard let self else { return }
            await self.endActivitiesNow(
                activities,
                snapshot: snapshot,
                dismissalPolicy: dismissalPolicy,
                reason: reason
            )
        }
    }

    private func endActivitiesNow(
        _ activities: [Activity<TransitWatchAttributes>],
        snapshot: SharedLiveActivitySnapshot?,
        dismissalPolicy: ActivityUIDismissalPolicy,
        reason: SharedLiveActivityEndReason
    ) async {
        let snapshotState = snapshot.map(makeState)
        for activity in activities {
            let finalState = makeFinalState(snapshotState ?? activity.content.state, reason: reason)
            desiredStateByCommuteId[activity.attributes.commuteId] = nil
            cancelTransitionRefresh(for: activity.attributes.commuteId)
            await activity.end(
                activityContent(state: finalState),
                dismissalPolicy: dismissalPolicy
            )
        }
    }

    private func makeFinalState(
        _ state: TransitWatchContentState,
        reason: SharedLiveActivityEndReason
    ) -> TransitWatchContentState {
        TransitWatchContentState(
            title: IosLiveActivityController.shared.endTitle(reason: reason, fallback: state.title),
            body: IosLiveActivityController.shared.endBody(reason: reason, fallback: state.body),
            lineLabel: state.lineLabel,
            directionHeadsign: state.directionHeadsign,
            walkingMinutes: state.walkingMinutes,
            departureDate: state.departureDate,
            windowOpenDate: state.windowOpenDate,
            finalCallDate: state.finalCallDate,
            statusRaw: IosLiveActivityController.shared.endStatusKey(reason: reason)
        )
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

    private func activityContent(state: TransitWatchContentState) -> ActivityContent<TransitWatchContentState> {
        ActivityContent(state: state, staleDate: staleDate(for: state))
    }

    private func staleDate(for state: TransitWatchContentState) -> Date? {
        nextTransitionDate(for: state, after: Date())
            ?? (isTerminalState(state) ? nil : max(state.departureDate.addingTimeInterval(120), Date().addingTimeInterval(30)))
    }

    private func scheduleTransitionRefresh(for commuteId: String, state: TransitWatchContentState) {
        cancelTransitionRefresh(for: commuteId)
        let transitionDates = transitionDates(for: state, after: Date())
        guard !transitionDates.isEmpty else {
            refreshTasksByCommuteId[commuteId] = nil
            return
        }

        refreshTasksByCommuteId[commuteId] = transitionDates.map { transitionDate in
            Task { [weak self] in
                let delaySeconds = max(transitionDate.timeIntervalSinceNow, 0)
                try? await Task.sleep(nanoseconds: UInt64(delaySeconds * 1_000_000_000))
                if Task.isCancelled { return }
                _ = await self?.refreshActivitiesForCommute(commuteId)
            }
        }
    }

    private func cancelTransitionRefresh(for commuteId: String) {
        refreshTasksByCommuteId[commuteId]?.forEach { $0.cancel() }
        refreshTasksByCommuteId[commuteId] = nil
    }

    private func refreshActivitiesForCommute(_ commuteId: String) async -> TransitWatchContentState? {
        let activities = activeActivities(matchingCommuteId: commuteId)
        guard !activities.isEmpty else {
            desiredStateByCommuteId[commuteId] = nil
            cancelTransitionRefresh(for: commuteId)
            return nil
        }

        let baseState = desiredStateByCommuteId[commuteId] ?? activities.first?.content.state
        guard let baseState else { return nil }
        let nextState = stateForCurrentTime(baseState)
        desiredStateByCommuteId[commuteId] = nextState

        for activity in activities {
            let alertConfiguration = makeAlertConfiguration(
                previousState: activity.content.state,
                nextState: nextState
            )
            await activity.update(
                activityContent(state: nextState),
                alertConfiguration: alertConfiguration
            )
        }
        return nextState
    }

    private func stateForCurrentTime(_ state: TransitWatchContentState) -> TransitWatchContentState {
        let statusRaw = statusRaw(for: state, at: Date())
        return TransitWatchContentState(
            title: title(for: statusRaw, fallback: state.title),
            body: state.body,
            lineLabel: state.lineLabel,
            directionHeadsign: state.directionHeadsign,
            walkingMinutes: state.walkingMinutes,
            departureDate: state.departureDate,
            windowOpenDate: state.windowOpenDate,
            finalCallDate: state.finalCallDate,
            statusRaw: statusRaw
        )
    }

    private func statusRaw(for state: TransitWatchContentState, at date: Date) -> String {
        if isTerminalState(state) {
            return state.statusRaw
        }
        if date >= state.departureDate {
            return "Missed"
        }
        if date >= state.finalCallDate {
            return "FinalCall"
        }
        if date >= state.windowOpenDate {
            return "LeaveNow"
        }
        return "GetReady"
    }

    private func title(for statusRaw: String, fallback: String) -> String {
        switch statusRaw {
        case "GetReady":
            return "Get ready"
        case "LeaveNow":
            return "Leave now"
        case "FinalCall":
            return "Final call"
        case "Missed":
            return "Open app for next chance"
        default:
            return fallback
        }
    }

    private func nextTransitionDate(for state: TransitWatchContentState, after date: Date) -> Date? {
        transitionDates(for: state, after: date).first
    }

    private func transitionDates(for state: TransitWatchContentState, after date: Date) -> [Date] {
        if isTerminalState(state) { return [] }
        return [state.windowOpenDate, state.finalCallDate, state.departureDate]
            .filter { date < $0 }
    }

    private func isTerminalState(_ state: TransitWatchContentState) -> Bool {
        state.statusRaw == "WatchStopped" || state.statusRaw == "Ended"
    }

    private func enqueueOperation(_ operation: @escaping () async -> Void) {
        let previousTask = operationTask
        let task = Task {
            await previousTask?.value
            await operation()
        }
        operationTask = task
    }

    private func logActivityError(_ action: String, _ error: Error) {
        #if DEBUG
        print("TransitLiveActivityBridge \(action) failed: \(error)")
        #endif
    }
}
