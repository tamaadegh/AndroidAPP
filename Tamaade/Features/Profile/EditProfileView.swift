import SwiftUI

/// "Edit my details": PATCH api/user/ with names, email and phone. Adding a phone turns on
/// SMS-code sign-in; adding an email turns on email + password sign-in. At least one must remain.
struct EditProfileView: View {
    @EnvironmentObject private var store: StoreModel
    @EnvironmentObject private var router: Router
    @Environment(\.dismiss) private var dismiss

    @State private var firstName = ""
    @State private var lastName = ""
    @State private var email = ""
    @State private var phone = ""
    @State private var loaded = false
    @State private var localError: String?

    var body: some View {
        ScrollView {
            if let user = store.user {
                form(user)
            }
        }
        .scrollDismissesKeyboard(.interactively)
        .background(Color.pageBackground)
        .navigationTitle("Edit my details")
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(store.savingProfile)
        .brandNavigationBar()
        .onAppear {
            store.clearProfileError()
            guard !loaded, let user = store.user else { return }
            firstName = user.firstName
            lastName = user.lastName
            email = user.email
            phone = user.phoneNumber
            loaded = true
        }
        // Signed out (e.g. session expired) while on this screen.
        .onChange(of: store.user == nil) { signedOut in
            if signedOut { dismiss() }
        }
    }

    private func form(_ user: UserSession) -> some View {
        let shownError = localError ?? store.profileError
        let formError = store.profileFieldErrors["non_field_errors"].flatMap {
            localError == nil && $0 != store.profileError ? $0 : nil
        }
        return VStack(spacing: 0) {
            VStack(alignment: .leading, spacing: 6) {
                Text("How you can sign in")
                    .font(.tTitle.weight(.bold))
                    .foregroundColor(.foreground)
                    .padding(.bottom, 2)
                SignInMethodRow(label: "SMS code sign-in", on: !user.phoneNumber.isBlank, hint: "add a phone number")
                SignInMethodRow(label: "Email + password sign-in", on: !user.email.isBlank, hint: "add an email")
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .padding(.bottom, 20)

            AuthErrorBlock(error: shownError)
            AuthErrorBlock(error: formError)
            BrandTextField(placeholder: "First name", systemImage: "person", text: edited($firstName),
                           contentType: .givenName, autocapitalization: .words)
            FieldError(message: store.profileFieldErrors["first_name"])
            BrandTextField(placeholder: "Last name", systemImage: "person", text: edited($lastName),
                           contentType: .familyName, autocapitalization: .words)
                .padding(.top, 14)
            FieldError(message: store.profileFieldErrors["last_name"])
            BrandTextField(placeholder: "Email", systemImage: "envelope", text: edited($email),
                           keyboard: .emailAddress, contentType: .emailAddress, autocapitalization: .never)
                .padding(.top, 14)
            FieldError(message: store.profileFieldErrors["email"])
            PhoneNumberField(text: edited($phone))
                .padding(.top, 14)
            FieldError(message: store.profileFieldErrors["phone_number"])
            Text("Leave a field empty to remove it. Keep at least an email or a phone number.")
                .font(.tBodySmall)
                .foregroundColor(.muted)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.top, 4)

            PrimaryButton(title: store.savingProfile ? "Saving…" : "Save",
                          loading: store.savingProfile, enabled: !store.savingProfile, action: save)
                .padding(.top, 20)
            Button("Cancel") { dismiss() }
                .foregroundColor(.muted)
                .disabled(store.savingProfile)
                .padding(.vertical, 12)
        }
        .padding(24)
    }

    private func edited(_ binding: Binding<String>) -> Binding<String> {
        Binding(get: { binding.wrappedValue }, set: {
            binding.wrappedValue = $0
            localError = nil
            store.clearProfileError()
        })
    }

    private func save() {
        let trimmedEmail = email.trimmingCharacters(in: .whitespacesAndNewlines)
        let normalizedPhone: String? = phone.isBlank ? "" : PhoneNumber.normalizeGhana(phone)
        if firstName.isBlank || lastName.isBlank {
            localError = "Enter your first and last name"
        } else if trimmedEmail.isEmpty && phone.isBlank {
            localError = "Enter an email or a phone number."
        } else if !trimmedEmail.isEmpty && !Validation.isEmail(trimmedEmail) {
            localError = "Enter a valid email"
        } else if normalizedPhone == nil {
            localError = "Enter a valid Ghana mobile number, e.g. 024 123 4567"
        } else {
            localError = nil
        }
        guard localError == nil, let normalizedPhone else { return }
        store.updateProfile(
            firstName: firstName.trimmingCharacters(in: .whitespacesAndNewlines),
            lastName: lastName.trimmingCharacters(in: .whitespacesAndNewlines),
            email: trimmedEmail,
            phone: normalizedPhone
        ) {
            dismiss()
            router.showToast("Details updated")
        }
    }
}

private struct SignInMethodRow: View {
    let label: String
    let on: Bool
    let hint: String

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: on ? "checkmark.circle" : "minus.circle")
                .foregroundColor(on ? .brandGreen : .muted)
            Text(on ? "\(label): on" : "\(label): off (\(hint))")
                .font(.tBody)
                .foregroundColor(on ? .foreground : .muted)
        }
    }
}
