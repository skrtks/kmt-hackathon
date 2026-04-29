import Combine
import Foundation
import WatchConnectivity
import WatchKit

final class WatchSessionModel: NSObject, ObservableObject, WCSessionDelegate {
    @Published private(set) var snapshot: WatchSnapshot?
    @Published private(set) var syncState: WatchSyncState = .loading

    private var hapticTimer: Timer?
    private var hapticsEnabled = true
    private var lastGroupId: String?
    private var lastStatus: WatchStatus?
    private var lastProgressPulseSecond: Int?
    private var lastGetReadyMinute: Int?
    private var lastIsLeaving: Bool?
    private var finalCallHapticBurstId: UUID?

    func activate() {
        loadStoredSnapshot()
        guard WCSession.isSupported() else {
            syncState = snapshot == nil ? .phoneUnavailable : .active
            return
        }

        let session = WCSession.default
        session.delegate = self
        session.activate()
        applyCurrentApplicationContextIfAvailable()
    }

    func setHapticsEnabled(_ enabled: Bool) {
        hapticsEnabled = enabled
        if !enabled {
            finalCallHapticBurstId = nil
        }
    }

    func session(
        _ session: WCSession,
        activationDidCompleteWith activationState: WCSessionActivationState,
        error: Error?
    ) {
        DispatchQueue.main.async {
            if activationState == .activated {
                self.applyCurrentApplicationContextIfAvailable()
            } else if self.snapshot == nil {
                self.syncState = .phoneUnavailable
            }
        }
    }

    func session(_ session: WCSession, didReceiveApplicationContext applicationContext: [String: Any]) {
        DispatchQueue.main.async {
            self.apply(applicationContext: applicationContext)
        }
    }

    private func applyCurrentApplicationContextIfAvailable() {
        let context = WCSession.default.applicationContext
        if context.isEmpty {
            syncState = snapshot == nil ? .phoneUnavailable : .active
        } else {
            apply(applicationContext: context)
        }
    }

    private func apply(applicationContext: [String: Any]) {
        guard applicationContext[WatchPayloadKey.active] as? Bool == true else {
            snapshot = nil
            syncState = .noActiveWatch
            clearStoredSnapshot()
            stopHapticTimer()
            resetHapticTracking()
            return
        }

        guard let nextSnapshot = WatchSnapshot(applicationContext: applicationContext) else {
            syncState = snapshot == nil ? .phoneUnavailable : .active
            return
        }

        let previousSnapshot = snapshot
        snapshot = nextSnapshot
        syncState = .active
        save(snapshot: nextSnapshot)
        startHapticTimer()
        playUpdateHapticIfNeeded(previous: previousSnapshot, next: nextSnapshot)
    }

    private func loadStoredSnapshot() {
        guard
            let data = UserDefaults.standard.data(forKey: storedSnapshotKey),
            let storedSnapshot = try? JSONDecoder().decode(WatchSnapshot.self, from: data)
        else {
            return
        }
        snapshot = storedSnapshot
        syncState = .active
        startHapticTimer()
    }

    private func save(snapshot: WatchSnapshot) {
        guard let data = try? JSONEncoder().encode(snapshot) else { return }
        UserDefaults.standard.set(data, forKey: storedSnapshotKey)
    }

    private func clearStoredSnapshot() {
        UserDefaults.standard.removeObject(forKey: storedSnapshotKey)
    }

    private func startHapticTimer() {
        if hapticTimer != nil { return }
        hapticTimer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { [weak self] _ in
            self?.tickHaptics()
        }
    }

    private func stopHapticTimer() {
        hapticTimer?.invalidate()
        hapticTimer = nil
    }

    private func resetHapticTracking() {
        lastGroupId = nil
        lastStatus = nil
        lastProgressPulseSecond = nil
        lastGetReadyMinute = nil
        lastIsLeaving = nil
        finalCallHapticBurstId = nil
    }

    private func playUpdateHapticIfNeeded(previous: WatchSnapshot?, next: WatchSnapshot) {
        guard hapticsEnabled, let previous else { return }
        if previous.groupId != next.groupId {
            resetHapticTracking()
            play(.selection)
            return
        }
        if !previous.isLeaving && next.isLeaving {
            play(.confirmation)
            lastIsLeaving = true
            return
        }
    }

    private func tickHaptics() {
        guard hapticsEnabled, let snapshot else { return }
        let nowSecondsOfDay = snapshot.currentSecondsOfDay()
        let status = snapshot.displayStatus(nowSecondsOfDay: nowSecondsOfDay)
        let progress = snapshot.leaveWindowElapsedFraction(nowSecondsOfDay: nowSecondsOfDay)
        let getReadyMinute = Self.getReadyHapticMinute(
            windowOpenMinutes: snapshot.windowOpenMinutes,
            nowSecondsOfDay: nowSecondsOfDay
        )
        let getReadyProgress = Self.getReadyHapticProgress(
            windowOpenMinutes: snapshot.windowOpenMinutes,
            nowSecondsOfDay: nowSecondsOfDay
        )

        if snapshot.isLeaving {
            if lastIsLeaving != true {
                play(.confirmation)
                lastIsLeaving = true
            }
            lastGroupId = snapshot.groupId
            lastStatus = status
            return
        }

        if lastGroupId != snapshot.groupId {
            lastGroupId = snapshot.groupId
            lastStatus = status
            lastProgressPulseSecond = nowSecondsOfDay
            lastGetReadyMinute = getReadyMinute
            if status == .leaveNow {
                performProgress(progress)
            } else if status == .finalCall {
                performFinalCallHaptic(for: snapshot)
            }
            return
        }

        if status != lastStatus {
            if status == .leaveNow {
                performProgress(progress)
            } else if status == .finalCall {
                performFinalCallHaptic(for: snapshot)
            }
            lastStatus = status
        }

        if status == .getReady,
           let getReadyMinute,
           getReadyMinute != lastGetReadyMinute {
            performProgress(getReadyProgress ?? 0)
            lastGetReadyMinute = getReadyMinute
        }

        if status == .leaveNow && nowSecondsOfDay != lastProgressPulseSecond {
            performProgress(progress)
            lastProgressPulseSecond = nowSecondsOfDay
        }
    }

    private func play(_ effect: WatchHapticEffect) {
        WKInterfaceDevice.current().play(effect.watchType)
    }

    private func performProgress(_ progress: Double) {
        switch progress {
        case 0.75...:
            play(.progressHigh)
        case 0.40...:
            play(.progressMedium)
        default:
            play(.progressTick)
        }
    }

    private func performFinalCallHaptic(for snapshot: WatchSnapshot) {
        let burstId = UUID()
        let groupId = snapshot.groupId
        finalCallHapticBurstId = burstId

        let pulses: [(delay: TimeInterval, effect: WatchHapticEffect)] = [
            (0.00, .critical),
            (0.18, .urgent),
            (0.42, .critical),
            (0.72, .urgent)
        ]

        for pulse in pulses {
            let action = { [weak self] in
                guard
                    let self,
                    self.hapticsEnabled,
                    self.finalCallHapticBurstId == burstId,
                    let current = self.snapshot,
                    current.groupId == groupId,
                    !current.isLeaving,
                    current.displayStatus(nowSecondsOfDay: current.currentSecondsOfDay()) == .finalCall
                else {
                    return
                }
                self.play(pulse.effect)
            }

            if pulse.delay == 0 {
                action()
            } else {
                DispatchQueue.main.asyncAfter(deadline: .now() + pulse.delay, execute: action)
            }
        }
    }

    private static func getReadyHapticMinute(windowOpenMinutes: Int, nowSecondsOfDay: Int) -> Int? {
        let secondsUntilOpen = secondsUntil(minutesOfDay: windowOpenMinutes, nowSecondsOfDay: nowSecondsOfDay)
        guard (1...180).contains(secondsUntilOpen) else { return nil }
        return min(max((secondsUntilOpen + 59) / secondsPerMinute, 1), 3)
    }

    private static func getReadyHapticProgress(windowOpenMinutes: Int, nowSecondsOfDay: Int) -> Double? {
        let secondsUntilOpen = secondsUntil(minutesOfDay: windowOpenMinutes, nowSecondsOfDay: nowSecondsOfDay)
        guard (1...180).contains(secondsUntilOpen) else { return nil }
        return min(max(1 - Double(secondsUntilOpen) / 180, 0), 1)
    }

    private static func secondsUntil(minutesOfDay: Int, nowSecondsOfDay: Int) -> Int {
        let rawSeconds = minutesOfDay * secondsPerMinute - nowSecondsOfDay
        return rawSeconds < 0 ? rawSeconds + secondsPerDay : rawSeconds
    }
}

private enum WatchHapticEffect {
    case selection
    case progressTick
    case progressMedium
    case progressHigh
    case confirmation
    case critical
    case urgent
    case error

    var watchType: WKHapticType {
        switch self {
        case .selection, .progressTick:
            return .click
        case .progressMedium:
            return .directionUp
        case .progressHigh:
            return .notification
        case .confirmation:
            return .success
        case .critical, .error:
            return .failure
        case .urgent:
            return .retry
        }
    }
}

private let storedSnapshotKey = "active_watch_snapshot"
