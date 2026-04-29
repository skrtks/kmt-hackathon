import SwiftUI

struct WaterCountdownBackground: View {
    let fraction: Double

    var body: some View {
        GeometryReader { geometry in
            let clamped = min(max(fraction, 0), 1)
            let waterHeight = geometry.size.height * (1 - clamped)
            let waterTop = geometry.size.height - waterHeight

            ZStack(alignment: .bottom) {
                Color.clear
                Rectangle()
                    .fill(Color(red: 0.01, green: 0.41, blue: 0.63))
                    .frame(height: max(waterHeight, 0))
                Rectangle()
                    .fill(Color(red: 0.22, green: 0.74, blue: 0.97))
                    .frame(height: 2)
                    .offset(y: -waterHeight + 1)
                    .opacity(waterTop < geometry.size.height ? 1 : 0)
            }
        }
        .ignoresSafeArea()
    }
}
struct FinalCallEdgeRing: View {
    let isVisible: Bool

    var body: some View {
        if isVisible {
            GeometryReader { geometry in
                TimelineView(.animation) { timeline in
                    let phase = (timeline.date.timeIntervalSinceReferenceDate.truncatingRemainder(dividingBy: 1.8)) / 1.8
                    let pulse = phase < 0.5 ? phase * 2 : (1 - phase) * 2
                    let inset = 5 + pulse
                    let cornerRadius = min(geometry.size.width, geometry.size.height) * 0.26
                    RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                        .strokeBorder(
                            Color(red: 0.96, green: 0.25, blue: 0.36).opacity(0.58 + 0.32 * pulse),
                            lineWidth: 4 + 2 * pulse
                        )
                        .shadow(
                            color: Color(red: 0.96, green: 0.25, blue: 0.36).opacity(0.38),
                            radius: 10 + 5 * pulse
                        )
                        .padding(inset)
                }
            }
            .ignoresSafeArea()
        }
    }
}
