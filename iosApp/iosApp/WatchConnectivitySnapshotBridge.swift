import ComposeApp
import Foundation
import WatchConnectivity

final class WatchConnectivitySnapshotBridge: NSObject, WCSessionDelegate {
    static let shared = WatchConnectivitySnapshotBridge()

    private var pendingContext: [String: Any]?
    private(set) var hasActiveSnapshot = false

    var isSupported: Bool {
        WCSession.isSupported()
    }

    private override init() {
        super.init()
        activateSessionIfNeeded()
    }

    @discardableResult
    func publish(snapshot: SharedLiveActivitySnapshot) -> Bool {
        var context = payload(from: snapshot)
        context[WatchPayloadKey.active] = true
        return updateApplicationContext(context)
    }

    @discardableResult
    func clear(snapshot: SharedLiveActivitySnapshot? = nil) -> Bool {
        var context: [String: Any] = [
            WatchPayloadKey.active: false,
            WatchPayloadKey.updatedAt: Date().timeIntervalSince1970
        ]
        if let snapshot {
            context.merge(payload(from: snapshot), uniquingKeysWith: { current, _ in current })
        }
        return updateApplicationContext(context)
    }

    private func payload(from snapshot: SharedLiveActivitySnapshot) -> [String: Any] {
        var payload: [String: Any] = [
            WatchPayloadKey.updatedAt: Date().timeIntervalSince1970,
            WatchPayloadKey.commuteId: snapshot.commuteId,
            WatchPayloadKey.groupId: snapshot.groupId,
            WatchPayloadKey.status: IosLiveActivityController.shared.statusKey(snapshot: snapshot),
            WatchPayloadKey.title: snapshot.title,
            WatchPayloadKey.body: snapshot.body,
            WatchPayloadKey.stopName: snapshot.stopName,
            WatchPayloadKey.lineLabel: snapshot.lineLabel,
            WatchPayloadKey.directionHeadsign: snapshot.directionHeadsign,
            WatchPayloadKey.departureTimeMinutes: Int(snapshot.departureTimeMinutes),
            WatchPayloadKey.windowOpenMinutes: Int(snapshot.windowOpenMinutes),
            WatchPayloadKey.finalCallMinutes: Int(snapshot.finalCallMinutes),
            WatchPayloadKey.walkingMinutes: Int(snapshot.walkingMinutes),
            WatchPayloadKey.isLeaving: snapshot.isLeaving
        ]
        if let syncedNowSecondsOfDay = snapshot.syncedNowSecondsOfDay {
            payload[WatchPayloadKey.syncedNowSeconds] = syncedNowSecondsOfDay.intValue
        }
        return payload
    }

    private func updateApplicationContext(_ context: [String: Any]) -> Bool {
        guard WCSession.isSupported() else { return false }
        activateSessionIfNeeded()
        pendingContext = context
        hasActiveSnapshot = (context[WatchPayloadKey.active] as? Bool) == true
        if WCSession.default.activationState != .activated {
            return true
        }
        flushPendingContext()
        return true
    }

    private func activateSessionIfNeeded() {
        guard WCSession.isSupported() else { return }
        let session = WCSession.default
        if session.delegate !== self {
            session.delegate = self
        }
        if session.activationState == .notActivated {
            session.activate()
        }
    }

    private func flushPendingContext() {
        guard let pendingContext else { return }
        do {
            try WCSession.default.updateApplicationContext(pendingContext)
            self.pendingContext = nil
        } catch {
            #if DEBUG
            print("WatchConnectivity updateApplicationContext failed: \(error)")
            #endif
        }
    }

    func session(
        _ session: WCSession,
        activationDidCompleteWith activationState: WCSessionActivationState,
        error: Error?
    ) {
        if activationState == .activated {
            flushPendingContext()
        }
    }

    func sessionDidBecomeInactive(_ session: WCSession) {}

    func sessionDidDeactivate(_ session: WCSession) {
        session.activate()
    }
}

enum WatchPayloadKey {
    static let active = "active"
    static let updatedAt = "updated_at"
    static let commuteId = "commute_id"
    static let groupId = "group_id"
    static let status = "status"
    static let title = "title"
    static let body = "body"
    static let stopName = "stop_name"
    static let lineLabel = "line_label"
    static let directionHeadsign = "direction_headsign"
    static let departureTimeMinutes = "departure_time_minutes"
    static let windowOpenMinutes = "window_open_minutes"
    static let finalCallMinutes = "final_call_minutes"
    static let walkingMinutes = "walking_minutes"
    static let isLeaving = "is_leaving"
    static let syncedNowSeconds = "synced_now_seconds"
}
