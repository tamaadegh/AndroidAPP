import SwiftUI

/// Rounded white text field with a leading icon (password fields get a show/hide toggle).
struct BrandTextField: View {
    let placeholder: String
    let systemImage: String
    @Binding var text: String
    var isSecure = false
    var showToggle = false
    var keyboard: UIKeyboardType = .default
    var contentType: UITextContentType? = nil
    var autocapitalization: TextInputAutocapitalization = .sentences

    @State private var revealed = false
    @FocusState private var focused: Bool

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: systemImage)
                .foregroundColor(.muted)
                .frame(width: 22)
            Group {
                if isSecure && !revealed {
                    SecureField(placeholder, text: $text)
                } else {
                    TextField(placeholder, text: $text)
                }
            }
            .focused($focused)
            .keyboardType(keyboard)
            .textContentType(contentType)
            .textInputAutocapitalization(isSecure ? .never : autocapitalization)
            .autocorrectionDisabled(isSecure || keyboard != .default)
            .foregroundColor(.foreground)
            if isSecure && showToggle {
                Button {
                    revealed.toggle()
                } label: {
                    Image(systemName: revealed ? "eye.slash" : "eye")
                        .foregroundColor(.muted)
                }
                .accessibilityLabel(revealed ? "Hide password" : "Show password")
            }
        }
        .padding(.horizontal, 14)
        .frame(height: 54)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(focused ? Color.brandGreen : Color.border, lineWidth: focused ? 2 : 1)
        )
    }
}

/// Ghana phone number field; keeps only digits and + - ( ) and spaces.
struct PhoneNumberField: View {
    @Binding var text: String
    var placeholder = "Phone number (e.g. 024 123 4567)"

    var body: some View {
        BrandTextField(
            placeholder: placeholder,
            systemImage: "phone",
            text: Binding(get: { text }, set: { text = PhoneNumber.filterInput($0) }),
            keyboard: .phonePad,
            contentType: .telephoneNumber
        )
    }
}

/// Full-width green button with an optional spinner.
struct PrimaryButton: View {
    let title: String
    var loading = false
    var enabled = true
    var color: Color = .brandGreen
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                if loading {
                    ProgressView().tint(.white)
                }
                Text(title).font(.system(size: 16, weight: .semibold))
            }
            .foregroundColor(.white)
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .background(enabled ? color : color.opacity(0.6))
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }
}

/// Server message for one field, shown right under it.
struct FieldError: View {
    let message: String?

    var body: some View {
        if let message, !message.isBlank {
            Text(message)
                .font(.tBodySmall)
                .foregroundColor(.errorRed)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.leading, 4)
                .padding(.top, 4)
        }
    }
}

/// Error text, plus an optional action under it (e.g. "Create an account" for an unknown number).
struct AuthErrorBlock: View {
    let error: String?
    var actionLabel: String? = nil
    var onAction: () -> Void = {}

    var body: some View {
        if let error {
            VStack(alignment: .leading, spacing: 8) {
                Text(error)
                    .font(.tBodySmall)
                    .foregroundColor(.errorRed)
                    .frame(maxWidth: .infinity, alignment: .leading)
                if let actionLabel {
                    Button(action: onAction) {
                        Text(actionLabel)
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(.brandGreen)
                            .frame(maxWidth: .infinity)
                            .frame(height: 44)
                            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.border))
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.bottom, 8)
        }
    }
}

/// "Didn't get it? Resend code" row with the countdown.
struct ResendRow: View {
    let seconds: Int
    let loading: Bool
    let onResend: () -> Void

    var body: some View {
        HStack(spacing: 4) {
            Text("Didn't get it?").foregroundColor(.muted)
            Button(seconds > 0 ? "Resend in \(seconds)s" : "Resend code", action: onResend)
                .font(.system(size: 15, weight: .bold))
                .foregroundColor(seconds > 0 ? .muted : .brandGreen)
                .disabled(loading || seconds > 0)
        }
        .font(.system(size: 15))
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
    }
}

/// Drives once-a-second redraws for the SMS-code countdowns.
struct CooldownClock: ViewModifier {
    @Binding var now: TimeInterval
    private let timer = Timer.publish(every: 0.5, on: .main, in: .common).autoconnect()

    func body(content: Content) -> some View {
        content.onReceive(timer) { _ in now = ProcessInfo.processInfo.systemUptime }
    }
}

extension View {
    func cooldownClock(_ now: Binding<TimeInterval>) -> some View {
        modifier(CooldownClock(now: now))
    }
}
