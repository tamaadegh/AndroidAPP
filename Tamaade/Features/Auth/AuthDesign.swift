import SwiftUI

/// Shared frame for Sign in / Sign up / Reset password: green brand header with a back arrow,
/// then the form on a white card that overlaps the header. Same design as Android's AuthScaffold.
struct AuthScaffold<Content: View, Footer: View>: View {
    let title: String
    var subtitle: String?
    let onBack: () -> Void
    var backEnabled = true
    @ViewBuilder var content: () -> Content
    @ViewBuilder var footer: () -> Footer

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                header
                VStack(alignment: .leading, spacing: 0, content: content)
                    .padding(20)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 20))
                    .shadow(color: .black.opacity(0.08), radius: 10, y: 3)
                    .padding(.horizontal, 16)
                    .padding(.top, -28)
                VStack(spacing: 0, content: footer)
                    .frame(maxWidth: .infinity)
                    .padding(.horizontal, 16)
                    .padding(.top, 12)
                    .padding(.bottom, 16)
            }
        }
        .scrollDismissesKeyboard(.interactively)
        .background(Color.pageBackground.ignoresSafeArea())
        .toolbar(.hidden, for: .navigationBar)
    }

    private var header: some View {
        ZStack(alignment: .topLeading) {
            VStack(spacing: 0) {
                TamaadeWordmark(height: 30)
                Text(title)
                    .font(.system(size: 24, weight: .bold))
                    .foregroundColor(.white)
                    .multilineTextAlignment(.center)
                    .padding(.top, 20)
                if let subtitle {
                    Text(subtitle)
                        .font(.tBody)
                        .foregroundColor(.white.opacity(0.82))
                        .multilineTextAlignment(.center)
                        .padding(.top, 6)
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.horizontal, 24)
            .padding(.top, 20)

            Button(action: onBack) {
                Image(systemName: "arrow.left")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(width: 44, height: 44)
                    .background(Color.white.opacity(0.14))
                    .clipShape(Circle())
            }
            .disabled(!backEnabled)
            .accessibilityLabel("Back")
            .padding(.leading, 8)
            .padding(.top, 8)
        }
        .padding(.bottom, 44)
        .background(
            UnevenBottomRounded(radius: 28)
                .fill(Color.brandGreen)
                .ignoresSafeArea(edges: .top)
        )
    }
}

extension AuthScaffold where Footer == EmptyView {
    init(title: String, subtitle: String?, onBack: @escaping () -> Void, backEnabled: Bool = true,
         @ViewBuilder content: @escaping () -> Content) {
        self.init(title: title, subtitle: subtitle, onBack: onBack, backEnabled: backEnabled,
                  content: content, footer: { EmptyView() })
    }
}

/// Rectangle with only the bottom corners rounded (iOS 16 has no UnevenRoundedRectangle).
private struct UnevenBottomRounded: Shape {
    let radius: CGFloat

    func path(in rect: CGRect) -> Path {
        Path(UIBezierPath(roundedRect: rect, byRoundingCorners: [.bottomLeft, .bottomRight],
                          cornerRadii: CGSize(width: radius, height: radius)).cgPath)
    }
}

/// Small label above a field.
struct FieldLabel: View {
    let text: String
    init(_ text: String) { self.text = text }

    var body: some View {
        Text(text)
            .font(.system(size: 14, weight: .semibold))
            .foregroundColor(.foreground)
            .padding(.bottom, 6)
    }
}

enum NoticeKind {
    case error, offline, unavailable, wait, server, success, info

    /// What kind of message an API error is, from the texts `APIError` produces.
    static func classify(_ message: String) -> NoticeKind {
        let lower = message.lowercased()
        if message == APIError.offlineMessage || message == APIError.timeoutMessage { return .offline }
        if message == APIError.unavailableMessage { return .unavailable }
        if message.hasPrefix(APIError.serverErrorPrefix) { return .server }
        if lower.contains("too many") || lower.contains("try again in") { return .wait }
        return .error
    }

    fileprivate var style: (icon: String, tint: Color, background: Color, title: String?) {
        switch self {
        case .error: return ("exclamationmark.circle", Color(hex: 0xB3261E), Color(hex: 0xFDECEA), nil)
        case .offline: return ("wifi.slash", Color(hex: 0x8A5A00), Color(hex: 0xFFF4E0), "You're offline")
        case .unavailable: return ("icloud.slash", Color(hex: 0x2D54A4), Color(hex: 0xEAF0FB), "Not available yet")
        case .wait: return ("hourglass", Color(hex: 0x8A5A00), Color(hex: 0xFFF4E0), "Please wait a moment")
        case .server: return ("icloud.slash", Color(hex: 0xB3261E), Color(hex: 0xFDECEA), "Our server had a problem")
        case .success: return ("checkmark.circle", .brandGreen, Color(hex: 0xE8F1EC), nil)
        case .info: return ("info.circle", .brandGreen, Color(hex: 0xE8F1EC), nil)
        }
    }
}

/// Message card used for every auth error and confirmation: icon, optional title, text and an
/// optional action (e.g. "Create an account", "Use email instead").
struct AuthNotice: View {
    let message: String?
    var kind: NoticeKind? = nil
    var title: String? = nil
    var actionLabel: String? = nil
    var onAction: () -> Void = {}

    var body: some View {
        if let message {
            let style = (kind ?? NoticeKind.classify(message)).style
            HStack(alignment: .top, spacing: 10) {
                Image(systemName: style.icon)
                    .font(.system(size: 18))
                    .foregroundColor(style.tint)
                VStack(alignment: .leading, spacing: 2) {
                    if let heading = title ?? style.title {
                        Text(heading)
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(style.tint)
                    }
                    Text(message)
                        .font(.system(size: 13))
                        .foregroundColor(.foreground.opacity(0.85))
                        .fixedSize(horizontal: false, vertical: true)
                    if let actionLabel {
                        Button(actionLabel, action: onAction)
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(style.tint)
                            .padding(.top, 6)
                    }
                }
                Spacer(minLength: 0)
            }
            .padding(12)
            .background(style.background)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(style.tint.opacity(0.18)))
            .padding(.bottom, 12)
            .transition(.opacity.combined(with: .move(edge: .top)))
        }
    }
}

/// Ghana mobile number: fixed "🇬🇭 +233" prefix, the 9 local digits shown grouped as 24 123 4567.
/// A leading 0 or 233 typed out of habit is dropped. The binding holds the 9 local digits.
struct GhanaPhoneField: View {
    @Binding var digits: String
    var isError = false
    var onSubmit: () -> Void = {}

    @FocusState private var focused: Bool

    var body: some View {
        HStack(spacing: 0) {
            Text("🇬🇭").font(.system(size: 18))
            Text("+233")
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(.foreground)
                .padding(.leading, 6)
            Rectangle().fill(Color.border).frame(width: 1, height: 24).padding(.horizontal, 10)
            TextField("24 123 4567", text: Binding(
                get: { PhoneNumber.groupLocal(PhoneNumber.ghanaLocalDigits(digits)) },
                set: { digits = PhoneNumber.ghanaLocalDigits($0) }
            ))
            .font(.system(size: 17, weight: .medium))
            .kerning(0.5)
            .keyboardType(.phonePad)
            .textContentType(.telephoneNumber)
            .foregroundColor(.foreground)
            .focused($focused)
            .onSubmit(onSubmit)
        }
        .padding(.horizontal, 14)
        .frame(height: 56)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .overlay(
            RoundedRectangle(cornerRadius: 14)
                .stroke(isError ? Color(hex: 0xB3261E) : focused ? Color.brandGreen : Color.border,
                        lineWidth: focused || isError ? 2 : 1)
        )
    }
}

/// Six boxes for the SMS code; one hidden text field underneath, so paste and SMS autofill work.
struct OtpBoxes: View {
    @Binding var code: String
    var isError = false
    var enabled = true

    @FocusState private var focused: Bool

    var body: some View {
        ZStack {
            TextField("", text: Binding(get: { code }, set: { code = PhoneNumber.filterCode($0) }))
                .keyboardType(.numberPad)
                .textContentType(.oneTimeCode)
                .focused($focused)
                .disabled(!enabled)
                .foregroundColor(.clear)
                .tint(.clear)
                .accessibilityLabel("SMS code")
            HStack(spacing: 8) {
                ForEach(0..<PhoneNumber.otpLength, id: \.self) { i in
                    box(i)
                }
            }
            .contentShape(Rectangle())
            .onTapGesture { focused = true }
            .accessibilityHidden(true)
        }
        .onAppear {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) { focused = true }
        }
    }

    private func box(_ i: Int) -> some View {
        let chars = Array(code)
        let char = i < chars.count ? String(chars[i]) : ""
        let last = PhoneNumber.otpLength - 1
        let active = focused && (i == chars.count || (i == last && chars.count == PhoneNumber.otpLength))
        let border: Color = isError ? Color(hex: 0xB3261E)
            : active ? .brandGreen
            : !char.isEmpty ? Color.brandGreen.opacity(0.5) : .border
        return Text(char)
            .font(.system(size: 24, weight: .bold))
            .foregroundColor(.foreground)
            .frame(maxWidth: .infinity)
            .aspectRatio(0.86, contentMode: .fit)
            .background(char.isEmpty ? Color.white : Color(hex: 0xF1F6F3))
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(border, lineWidth: active ? 2 : 1))
    }
}

/// "or" separator between two ways of signing in.
struct OrDivider: View {
    var body: some View {
        HStack(spacing: 12) {
            Rectangle().fill(Color.border).frame(height: 1)
            Text("or").font(.tBodySmall).foregroundColor(.muted)
            Rectangle().fill(Color.border).frame(height: 1)
        }
        .padding(.vertical, 14)
    }
}

/// Outlined button that switches between phone and email sign-in.
struct SwitchMethodButton: View {
    let title: String
    let systemImage: String
    var enabled = true
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                Image(systemName: systemImage)
                Text(title).font(.system(size: 15, weight: .semibold))
            }
            .foregroundColor(.foreground)
            .frame(maxWidth: .infinity)
            .frame(height: 50)
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.border))
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }
}

/// Step 2 of phone sign-in: enter the 6-digit SMS code. Verifies automatically once all digits
/// are typed; `onVerify` is also wired to the button for retries.
struct OtpCodeStep: View {
    let phone: String
    @Binding var code: String
    let loading: Bool
    let error: String?
    let info: String?
    let resendSeconds: Int
    let verifyLabel: String
    let onVerify: (String) -> Void
    let onResend: () -> Void
    let onChangeNumber: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            CodeSentRow(phone: phone, loading: loading, onChange: onChangeNumber)
            OtpBoxes(
                code: Binding(get: { code }, set: { digits in
                    let completed = digits.count == PhoneNumber.otpLength && digits != code
                    code = digits
                    if completed && !loading { onVerify(digits) }
                }),
                isError: error != nil,
                enabled: !loading
            )
            .padding(.bottom, 16)
            AuthNotice(message: error)
            AuthNotice(message: error == nil ? info : nil, kind: .success)
            PrimaryButton(title: loading ? "Verifying…" : verifyLabel, loading: loading,
                          enabled: !loading && code.count == PhoneNumber.otpLength) {
                onVerify(code)
            }
            ResendRow(seconds: resendSeconds, loading: loading, onResend: onResend)
                .padding(.top, 4)
        }
    }
}

/// "Code sent by SMS to +233 24 123 4567   Change".
struct CodeSentRow: View {
    let phone: String
    let loading: Bool
    let onChange: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text("Code sent by SMS to").font(.tBody).foregroundColor(.muted)
            HStack {
                Text(PhoneNumber.formatGhana(phone))
                    .font(.system(size: 17, weight: .bold))
                    .foregroundColor(.foreground)
                Spacer()
                Button("Change", action: onChange)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(.brandGreen)
                    .disabled(loading)
            }
        }
        .padding(.bottom, 12)
    }
}
