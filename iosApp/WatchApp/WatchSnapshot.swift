import Foundation

enum WatchStatus: String, Codable {
    case getReady = "GetReady"
    case leaveNow = "LeaveNow"
    case finalCall = "FinalCall"
    case missed = "Missed"
}

struct WatchSnapshot: Codable, Equatable {
    let commuteId: String
    let groupId: String
    let status: WatchStatus
    let title: String
    let body: String
    let stopName: String
    let lineLabel: String
    let directionHeadsign: String
    let departureTimeMinutes: Int
    let windowOpenMinutes: Int
    let finalCallMinutes: Int
    let walkingMinutes: Int
    let isLeaving: Bool
    let syncedNowSecondsOfDay: Int?
    let receivedAt: Date

    init?(applicationContext: [String: Any]) {
        guard
            let commuteId = applicationContext[WatchPayloadKey.commuteId] as? String,
            let groupId = applicationContext[WatchPayloadKey.groupId] as? String,
            let statusRaw = applicationContext[WatchPayloadKey.status] as? String,
            let status = WatchStatus(rawValue: statusRaw),
            let title = applicationContext[WatchPayloadKey.title] as? String,
            let body = applicationContext[WatchPayloadKey.body] as? String,
            let stopName = applicationContext[WatchPayloadKey.stopName] as? String,
            let lineLabel = applicationContext[WatchPayloadKey.lineLabel] as? String,
            let directionHeadsign = applicationContext[WatchPayloadKey.directionHeadsign] as? String,
            let departureTimeMinutes = applicationContext[WatchPayloadKey.departureTimeMinutes] as? Int,
            let windowOpenMinutes = applicationContext[WatchPayloadKey.windowOpenMinutes] as? Int,
            let finalCallMinutes = applicationContext[WatchPayloadKey.finalCallMinutes] as? Int,
            let walkingMinutes = applicationContext[WatchPayloadKey.walkingMinutes] as? Int
        else {
            return nil
        }

        self.commuteId = commuteId
        self.groupId = groupId
        self.status = status
        self.title = title
        self.body = body
        self.stopName = stopName
        self.lineLabel = lineLabel
        self.directionHeadsign = directionHeadsign
        self.departureTimeMinutes = departureTimeMinutes
        self.windowOpenMinutes = windowOpenMinutes
        self.finalCallMinutes = finalCallMinutes
        self.walkingMinutes = walkingMinutes
        self.isLeaving = applicationContext[WatchPayloadKey.isLeaving] as? Bool ?? false
        self.syncedNowSecondsOfDay = applicationContext[WatchPayloadKey.syncedNowSeconds] as? Int
        self.receivedAt = Date()
    }

    var routeText: String {
        "\(lineLabel) to \(directionHeadsign)"
    }

    func headline(nowSecondsOfDay: Int) -> String {
        if isLeaving {
            return "Departure in \(Self.countdownText(targetMinutes: departureTimeMinutes, nowSecondsOfDay: nowSecondsOfDay))"
        }

        switch displayStatus(nowSecondsOfDay: nowSecondsOfDay) {
        case .getReady:
            return "Leave at \(Self.timeText(minutesOfDay: windowOpenMinutes))"
        case .leaveNow:
            return "Leave now"
        case .finalCall:
            return "Final call"
        case .missed:
            return "Next chance"
        }
    }

    func leaveWindowElapsedFraction(nowSecondsOfDay: Int) -> Double {
        switch displayStatus(nowSecondsOfDay: nowSecondsOfDay) {
        case .getReady:
            return 0
        case .finalCall, .missed:
            return 1
        case .leaveNow:
            let openSeconds = windowOpenMinutes * secondsPerMinute
            let finalSeconds = finalCallMinutes * secondsPerMinute
            let totalSeconds = max(Self.secondsBetween(start: openSeconds, end: finalSeconds), 1)
            let elapsedSeconds = Self.elapsedSecondsInWindow(
                start: openSeconds,
                end: finalSeconds,
                now: nowSecondsOfDay
            )
            return min(max(Double(elapsedSeconds) / Double(totalSeconds), 0), 1)
        }
    }

    func displayStatus(nowSecondsOfDay: Int) -> WatchStatus {
        if isLeaving {
            return status
        }

        let openSeconds = windowOpenMinutes * secondsPerMinute
        let finalSeconds = finalCallMinutes * secondsPerMinute
        let departureSeconds = departureTimeMinutes * secondsPerMinute
        if Self.isBetween(start: openSeconds, end: finalSeconds, now: nowSecondsOfDay) {
            return .leaveNow
        }
        if Self.isBetween(start: finalSeconds, end: departureSeconds, now: nowSecondsOfDay) {
            return .finalCall
        }

        let secondsUntilOpen = Self.secondsBetween(start: nowSecondsOfDay, end: openSeconds)
        let secondsSinceDeparture = Self.secondsBetween(start: departureSeconds, end: nowSecondsOfDay)
        return secondsUntilOpen < secondsSinceDeparture ? .getReady : .missed
    }

    func currentSecondsOfDay(at date: Date = Date()) -> Int {
        if let syncedNowSecondsOfDay {
            let elapsed = Int(date.timeIntervalSince(receivedAt))
            return (syncedNowSecondsOfDay + elapsed).positiveModulo(secondsPerDay)
        }

        let components = Calendar.current.dateComponents([.hour, .minute, .second], from: date)
        return ((components.hour ?? 0) * minutesPerHour + (components.minute ?? 0)) * secondsPerMinute +
            (components.second ?? 0)
    }

    static func timeText(minutesOfDay: Int) -> String {
        let calendar = Calendar.current
        let startOfDay = calendar.startOfDay(for: Date())
        let date = startOfDay.addingTimeInterval(TimeInterval(minutesOfDay * secondsPerMinute))
        return date.formatted(date: .omitted, time: .shortened)
    }

    static func countdownText(targetMinutes: Int, nowSecondsOfDay: Int) -> String {
        let remainingSeconds = max(targetMinutes * secondsPerMinute - nowSecondsOfDay, 0)
        let hours = remainingSeconds / (minutesPerHour * secondsPerMinute)
        let minutes = (remainingSeconds % (minutesPerHour * secondsPerMinute)) / secondsPerMinute
        let seconds = remainingSeconds % secondsPerMinute
        if hours > 0 {
            return "\(hours)h \(String(format: "%02d", minutes))m"
        }
        return "\(minutes):\(String(format: "%02d", seconds))"
    }

    private static func secondsBetween(start: Int, end: Int) -> Int {
        (end - start).positiveModulo(secondsPerDay)
    }

    private static func isBetween(start: Int, end: Int, now: Int) -> Bool {
        let duration = secondsBetween(start: start, end: end)
        if duration == 0 {
            return now == start
        }
        return secondsBetween(start: start, end: now) < duration
    }

    private static func elapsedSecondsInWindow(start: Int, end: Int, now: Int) -> Int {
        let totalSeconds = max(secondsBetween(start: start, end: end), 1)
        let elapsedSeconds = secondsBetween(start: start, end: now)
        if elapsedSeconds <= totalSeconds {
            return elapsedSeconds
        }

        let secondsUntilStart = secondsBetween(start: now, end: start)
        let secondsSinceEnd = secondsBetween(start: end, end: now)
        return secondsUntilStart < secondsSinceEnd ? 0 : totalSeconds
    }
}

enum WatchSyncState {
    case loading
    case noActiveWatch
    case phoneUnavailable
    case active
}

enum WatchPayloadKey {
    static let active = "active"
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

let minutesPerHour = 60
let secondsPerMinute = 60
let secondsPerDay = 24 * minutesPerHour * secondsPerMinute

private extension Int {
    func positiveModulo(_ divisor: Int) -> Int {
        let remainder = self % divisor
        return remainder < 0 ? remainder + divisor : remainder
    }
}
