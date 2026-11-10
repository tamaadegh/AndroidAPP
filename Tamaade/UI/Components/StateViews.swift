import SwiftUI

struct LoadingState: View {
    var body: some View {
        ProgressView()
            .tint(.brandGreen)
            .controlSize(.large)
            .frame(maxWidth: .infinity)
            .padding(48)
    }
}

struct ErrorState: View {
    let message: String
    let onRetry: () -> Void

    var body: some View {
        MessageState(systemImage: "icloud.slash", title: "Couldn't load the store", message: message,
                     actionLabel: "Try again", onAction: onRetry)
    }
}

struct EmptyState: View {
    let title: String
    let message: String
    var systemImage = "storefront"
    var actionLabel: String? = nil
    var onAction: () -> Void = {}

    var body: some View {
        MessageState(systemImage: systemImage, title: title, message: message,
                     actionLabel: actionLabel, onAction: onAction)
    }
}

struct MessageState: View {
    let systemImage: String
    let title: String
    let message: String
    var actionLabel: String?
    var onAction: () -> Void = {}

    var body: some View {
        VStack(spacing: 0) {
            Image(systemName: systemImage)
                .font(.system(size: 48))
                .foregroundColor(.muted)
                .frame(height: 56)
            Text(title)
                .font(.tTitleLarge.weight(.bold))
                .foregroundColor(.foreground)
                .multilineTextAlignment(.center)
                .padding(.top, 12)
            Text(message)
                .font(.tBody)
                .foregroundColor(.muted)
                .multilineTextAlignment(.center)
                .padding(.top, 6)
            if let actionLabel {
                Button(actionLabel, action: onAction)
                    .buttonStyle(.borderedProminent)
                    .tint(.brandGreen)
                    .padding(.top, 20)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 32)
        .padding(.vertical, 48)
    }
}
