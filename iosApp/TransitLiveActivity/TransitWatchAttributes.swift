import ActivityKit
import Foundation

@available(iOS 16.2, *)
public struct TransitWatchAttributes: ActivityAttributes {
    public typealias ContentState = TransitWatchContentState

    public let commuteId: String
    public let stopName: String

    public init(commuteId: String, stopName: String) {
        self.commuteId = commuteId
        self.stopName = stopName
    }
}

public struct TransitWatchContentState: Codable, Hashable {
    public let title: String
    public let body: String
    public let lineLabel: String
    public let directionHeadsign: String
    public let walkingMinutes: Int
    public let departureDate: Date
    public let windowOpenDate: Date
    public let finalCallDate: Date
    public let statusRaw: String

    public init(
        title: String,
        body: String,
        lineLabel: String,
        directionHeadsign: String,
        walkingMinutes: Int,
        departureDate: Date,
        windowOpenDate: Date,
        finalCallDate: Date,
        statusRaw: String
    ) {
        self.title = title
        self.body = body
        self.lineLabel = lineLabel
        self.directionHeadsign = directionHeadsign
        self.walkingMinutes = walkingMinutes
        self.departureDate = departureDate
        self.windowOpenDate = windowOpenDate
        self.finalCallDate = finalCallDate
        self.statusRaw = statusRaw
    }
}
