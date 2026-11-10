import SwiftUI

struct ProfileView: View {
    @EnvironmentObject private var store: StoreModel
    @EnvironmentObject private var router: Router

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                header

                if store.user == nil {
                    PrimaryButton(title: "Sign in / Register") { router.presentLogin() }
                } else {
                    Button {
                        store.logout()
                        router.showToast("Signed out")
                    } label: {
                        Text("Sign out")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(.brandGreen)
                            .frame(maxWidth: .infinity)
                            .frame(height: 44)
                            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.border))
                    }
                    .buttonStyle(.plain)
                }

                Button { router.open(.cart) } label: {
                    SettingsRow(systemImage: "cart", title: "My basket") { Chevron() }
                }
                .buttonStyle(.plain)
                Button { router.push(.privacy) } label: {
                    SettingsRow(systemImage: "hand.raised", title: "Privacy Policy") { Chevron() }
                }
                .buttonStyle(.plain)

                sectionTitle("Settings")
                SettingsRow(systemImage: "music.note", title: "Background music",
                            subtitle: store.musicEnabled ? "Plays while you shop" : "Off") {
                    Toggle("Background music", isOn: Binding(
                        get: { store.musicEnabled },
                        set: { store.setMusicEnabled($0) }
                    ))
                    .labelsHidden()
                    .tint(.brandGreen)
                }

                if store.user != nil {
                    sectionTitle("Account")
                    Button { router.push(.editProfile) } label: {
                        SettingsRow(systemImage: "pencil", title: "Edit my details",
                                    subtitle: "Name, email and phone number") { Chevron() }
                    }
                    .buttonStyle(.plain)
                    Button { router.push(.deleteAccount) } label: {
                        SettingsRow(systemImage: "trash", title: "Delete account",
                                    tint: .errorRed, titleColor: .errorRed) { Chevron() }
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(16)
        }
        .background(Color.pageBackground)
    }

    private var header: some View {
        HStack(spacing: 16) {
            Image(systemName: "person")
                .font(.system(size: 26))
                .foregroundColor(.white)
                .frame(width: 56, height: 56)
                .background(Color.white.opacity(0.2))
                .clipShape(Circle())
            VStack(alignment: .leading, spacing: 2) {
                if let user = store.user {
                    Text(user.displayName)
                        .font(.tTitleLarge.weight(.bold))
                    ForEach([user.phoneNumber, user.email].filter { !$0.isBlank && $0 != user.displayName },
                            id: \.self) { contact in
                        Text(contact)
                            .font(.tBody)
                            .opacity(0.85)
                    }
                } else {
                    Text("Welcome to Tamaade")
                        .font(.tTitleLarge.weight(.bold))
                    Text("Sign in to sync basket & orders")
                        .font(.tBody)
                        .opacity(0.85)
                }
            }
            .foregroundColor(.white)
            Spacer(minLength: 0)
        }
        .padding(20)
        .background(Color.brandGreen)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func sectionTitle(_ text: String) -> some View {
        Text(text)
            .font(.tTitle.weight(.bold))
            .foregroundColor(.foreground)
            .padding(.top, 8)
            .padding(.leading, 4)
    }
}
