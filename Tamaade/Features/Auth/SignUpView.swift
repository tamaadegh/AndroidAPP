import SwiftUI

/// Create an account (POST api/user/register/): names, email and/or phone, and a password.
/// The account is signed in straight away. A phone number lets the user sign in with an SMS
/// code later; an email lets them sign in with the password.
struct SignUpView: View {
    @EnvironmentObject private var store: StoreModel
    @EnvironmentObject private var router: Router

    @State private var firstName = ""
    @State private var lastName = ""
    @State private var email = ""
    @State private var phone: String
    @State private var password = ""
    @State private var confirmPassword = ""
    @State private var localError: String?

    init(initialPhone: String) {
        _phone = State(initialValue: initialPhone)
    }

    private var fieldErrors: [String: String] { store.authFieldErrors }

    var body: some View {
        let shownError = localError ?? store.authError
        let formError = fieldErrors["non_field_errors"].flatMap {
            localError == nil && $0 != store.authError ? $0 : nil
        }
        ScrollView {
            VStack(spacing: 0) {
                AuthHeader(title: "Create your Tamaade account",
                           subtitle: "Shop online in Ghana — Quality + Speed.")
                AuthErrorBlock(error: shownError)
                AuthErrorBlock(error: formError)
                BrandTextField(placeholder: "First name", systemImage: "person", text: edited($firstName),
                               contentType: .givenName, autocapitalization: .words)
                FieldError(message: fieldErrors["first_name"])
                BrandTextField(placeholder: "Last name", systemImage: "person", text: edited($lastName),
                               contentType: .familyName, autocapitalization: .words)
                    .padding(.top, 14)
                FieldError(message: fieldErrors["last_name"])
                BrandTextField(placeholder: "Email", systemImage: "envelope", text: edited($email),
                               keyboard: .emailAddress, contentType: .emailAddress, autocapitalization: .never)
                    .padding(.top, 14)
                FieldError(message: fieldErrors["email"])
                PhoneNumberField(text: edited($phone))
                    .padding(.top, 14)
                FieldError(message: fieldErrors["phone_number"])
                hint("Add an email to sign in with your password, or a phone number to sign in with an SMS code — or both.")
                BrandTextField(placeholder: "Password", systemImage: "lock", text: edited($password),
                               isSecure: true, showToggle: true, contentType: .newPassword)
                    .padding(.top, 14)
                FieldError(message: fieldErrors["password"])
                BrandTextField(placeholder: "Confirm password", systemImage: "lock", text: edited($confirmPassword),
                               isSecure: true, contentType: .newPassword)
                    .padding(.top, 14)
                hint("At least \(Validation.minPasswordLength) characters.")
                PrimaryButton(title: store.authLoading ? "Creating account…" : "Create account",
                              loading: store.authLoading, enabled: !store.authLoading, action: submit)
                    .padding(.top, 16)
                HStack(spacing: 4) {
                    Text("By signing up you agree to our").foregroundColor(.muted)
                    Button("Privacy Policy") { router.authPath.append(.privacy) }
                        .foregroundColor(.brandGreen)
                }
                .font(.tBodySmall)
                .padding(.top, 12)
                HStack(spacing: 4) {
                    Text("Already have an account?").foregroundColor(.muted)
                    Button("Sign in") {
                        edited()
                        router.showLogin(phone: phone.trimmingCharacters(in: .whitespaces))
                    }
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(.brandGreen)
                }
                .font(.system(size: 15))
                .padding(.top, 12)
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 32)
        }
        .scrollDismissesKeyboard(.interactively)
        .background(Color.pageBackground)
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { store.clearAuthError() }
    }

    private func hint(_ text: String) -> some View {
        Text(text)
            .font(.tBodySmall)
            .foregroundColor(.muted)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.top, 4)
    }

    private func edited(_ binding: Binding<String>) -> Binding<String> {
        Binding(get: { binding.wrappedValue }, set: {
            binding.wrappedValue = $0
            edited()
        })
    }

    private func edited() {
        localError = nil
        store.clearAuthError()
    }

    private func submit() {
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
        } else if password.count < Validation.minPasswordLength {
            localError = "Password must be at least \(Validation.minPasswordLength) characters"
        } else if password != confirmPassword {
            localError = "Passwords don't match"
        } else {
            localError = nil
        }
        guard localError == nil, let normalizedPhone else { return }
        store.register(
            firstName: firstName.trimmingCharacters(in: .whitespacesAndNewlines),
            lastName: lastName.trimmingCharacters(in: .whitespacesAndNewlines),
            password: password,
            email: trimmedEmail,
            phone: normalizedPhone
        ) {
            router.signedIn("Account created — welcome to Tamaade!")
        }
    }
}
