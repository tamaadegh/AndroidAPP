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
        _phone = State(initialValue: PhoneNumber.ghanaLocalDigits(initialPhone))
    }

    private var fieldErrors: [String: String] { store.authFieldErrors }

    var body: some View {
        let shownError = localError ?? store.authError
        let formError = fieldErrors["non_field_errors"].flatMap {
            localError == nil && $0 != store.authError ? $0 : nil
        }
        AuthScaffold(
            title: "Create your account",
            subtitle: "Shop online in Ghana — Quality + Speed.",
            onBack: { router.authBack() },
            content: {
                HStack(alignment: .top, spacing: 10) {
                    VStack(alignment: .leading, spacing: 0) {
                        FieldLabel("First name")
                        BrandTextField(placeholder: "Ama", systemImage: "person", text: edited($firstName),
                                       contentType: .givenName, autocapitalization: .words)
                        FieldError(message: fieldErrors["first_name"])
                    }
                    VStack(alignment: .leading, spacing: 0) {
                        FieldLabel("Last name")
                        BrandTextField(placeholder: "Mensah", systemImage: "person", text: edited($lastName),
                                       contentType: .familyName, autocapitalization: .words)
                        FieldError(message: fieldErrors["last_name"])
                    }
                }
                FieldLabel("Phone number").padding(.top, 14)
                GhanaPhoneField(digits: edited($phone), isError: fieldErrors["phone_number"] != nil)
                FieldError(message: fieldErrors["phone_number"])
                FieldLabel("Email").padding(.top, 14)
                BrandTextField(placeholder: "you@example.com", systemImage: "envelope", text: edited($email),
                               keyboard: .emailAddress, contentType: .emailAddress, autocapitalization: .never)
                FieldError(message: fieldErrors["email"])
                hint("Phone lets you sign in with an SMS code, email with your password. One of the two is enough.")
                FieldLabel("Password").padding(.top, 14)
                BrandTextField(placeholder: "At least \(Validation.minPasswordLength) characters", systemImage: "lock",
                               text: edited($password), isSecure: true, showToggle: true, contentType: .newPassword)
                FieldError(message: fieldErrors["password"])
                FieldLabel("Confirm password").padding(.top, 14)
                BrandTextField(placeholder: "Type it again", systemImage: "lock", text: edited($confirmPassword),
                               isSecure: true, contentType: .newPassword)
                    .padding(.bottom, 18)
                // Errors sit right above the button, where the user is looking after tapping it.
                AuthNotice(
                    message: shownError,
                    title: localError == nil && shownError.map({ NoticeKind.classify($0) == .error }) == true
                        ? "Couldn't create your account" : nil
                )
                AuthNotice(message: formError)
                PrimaryButton(title: store.authLoading ? "Creating account…" : "Create account",
                              loading: store.authLoading, enabled: !store.authLoading, action: submit)
                HStack(spacing: 4) {
                    Text("By signing up you agree to our").foregroundColor(.muted)
                    Button("Privacy Policy") { router.authPath.append(.privacy) }
                        .foregroundColor(.brandGreen)
                }
                .font(.tBodySmall)
                .frame(maxWidth: .infinity)
                .padding(.top, 12)
            },
            footer: {
                HStack(spacing: 4) {
                    Text("Already have an account?").foregroundColor(.muted)
                    Button("Sign in") {
                        edited()
                        router.showLogin(phone: PhoneNumber.normalizeGhana(phone) ?? phone)
                    }
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(.brandGreen)
                }
                .font(.system(size: 15))
                .padding(.vertical, 8)
            }
        )
        .animation(.easeOut(duration: 0.2), value: shownError)
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
            localError = "Enter your first and last name."
        } else if trimmedEmail.isEmpty && phone.isBlank {
            localError = "Add a phone number or an email so you can sign in."
        } else if !trimmedEmail.isEmpty && !Validation.isEmail(trimmedEmail) {
            localError = "That email doesn't look right. Check it and try again."
        } else if normalizedPhone == nil {
            localError = "That number doesn't look right. Enter a Ghana mobile number, e.g. 24 123 4567."
        } else if password.count < Validation.minPasswordLength {
            localError = "Your password needs at least \(Validation.minPasswordLength) characters."
        } else if password != confirmPassword {
            localError = "The two passwords don't match."
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
