# Security Section Recommendations

## Executive recommendation

The Security section should help users protect their account, understand where it is being used, and recover access safely. For Veltrix, the first release should focus on password management, active sessions, security alerts, and account recovery. Stronger controls such as two-factor authentication and passkeys can follow once the core flows are reliable.

## Complete security menu

| Setting | Applies to | Priority | What it should do |
|---|---|---:|---|
| Change password | Email/password accounts | High | Let users change their password after confirming the current password and meeting password rules. |
| Forgot password and recovery | All email accounts | High | Send a time-limited reset link or code without revealing whether an account exists. |
| Two-factor authentication | Email/password accounts | High | Add an authenticator-app factor, with backup codes and a safe recovery flow. |
| Passkeys | All supported accounts | Medium | Allow biometric or device-based sign-in for faster, phishing-resistant access. |
| Active sessions and devices | All accounts | High | List signed-in devices with platform, approximate location, last activity, and a revoke action. |
| Sign out of all devices | All accounts | High | Invalidate every active session after confirmation. |
| Login activity | All accounts | Medium | Show recent successful and failed sign-ins, device type, timestamp, and approximate location. |
| Security notifications | All accounts | Medium | Notify users when important security settings or sessions change. |
| Connected accounts | Google and future providers | Medium | Show linked providers and allow unlinking only when another recovery method exists. |
| Recovery methods | All accounts | Medium | Manage verified email, recovery email, phone number, and backup codes. |
| Delete account | All accounts | High | Place under a separated danger area with re-authentication and explicit confirmation. |
| Privacy and data controls | All accounts | Medium | Link to data export, account information download, and chat-history retention controls. |

## Recommended page structure

### Password and sign-in

Include Change password, Forgot password, Two-factor authentication, Passkeys, and Connected accounts. These are the controls users expect when managing how they sign in.

### Devices and activity

Include Active sessions, Sign out of all devices, and Login activity. This group helps users identify suspicious access and quickly contain it.

### Recovery and notifications

Include Recovery methods, Backup codes, and Security notifications. These controls reduce the chance that a user becomes permanently locked out of the account.

### Account and data

Include Delete account, Export data, and Privacy controls. Keep destructive actions visually separated from routine security settings.

## Detailed behavior requirements

### Change password

- Require the current password for an authenticated change.
- Reject weak and commonly breached passwords.
- Provide password visibility controls.
- Show password requirements before submission.
- Sign out other sessions after success, or clearly tell the user that existing sessions remain active.
- Send a confirmation notification after the change.

### Password reset

- Use a time-limited, single-use reset link or code.
- Do not reveal whether an email belongs to a Veltrix account.
- Rate-limit reset attempts.
- Invalidate old reset links after a successful reset.
- Notify the user when a reset is completed.

### Two-factor authentication

- Prefer authenticator-app codes over email OTP.
- Use a setup wizard with a QR code and manual setup key.
- Require a verification code before activation.
- Generate one-time backup codes and ask the user to store them safely.
- Require fresh authentication before disabling MFA.
- Notify the user when MFA is enabled, disabled, or used from a new device.

### Active sessions and devices

Each session should show:

- Device name or platform
- Browser or app version when available
- Approximate location
- Last active time
- Current-device indicator
- Revoke button

Revoking a session should take effect immediately. The current session should be clearly marked so users do not accidentally sign themselves out.

### Login activity

Show recent successful and failed sign-ins in plain language. Each item can include date, time, device, platform, and approximate location. Add two actions: “This was me” and “Secure my account.” Avoid exact location data when it creates unnecessary privacy risk.

### Security notifications

Notify users about:

- New sign-in
- Password change
- Password reset request and completion
- Email-address change
- MFA enrollment or removal
- Passkey enrollment or removal
- New device or session
- Session revocation
- Account deletion request

### Connected accounts

Show Google and any future sign-in providers. Users should only be able to unlink a provider if another verified sign-in or recovery method remains. Clearly explain the effect before unlinking.

### Recovery methods

Support verified email, recovery email, phone number, authenticator setup, and backup codes. Every recovery-method change should require re-authentication and notify the account owner.

### Delete account

Place this action in a visually separated danger area. Require recent authentication, explain what will be deleted, provide data export first, and use a confirmation step that makes accidental deletion difficult.

## Suggested screen copy

- **Security:** Protect your account and manage how you sign in.
- **Change password:** Choose a strong password that you do not use elsewhere.
- **Active sessions:** Review where your account is signed in.
- **Two-factor authentication:** Add an extra layer of protection when you sign in.
- **Sign out of all devices:** This will sign you out everywhere except this device.
- **Login activity:** Review recent sign-ins to your account.
- **Recovery methods:** Keep your account recoverable if you lose access to a device.
- **Delete account:** Deleting your account is permanent. Download anything you want to keep first.

## Recommended rollout

### Release 1

Change password, password reset, active sessions, sign out of all devices, login activity, and security notifications.

### Release 2

Authenticator-based two-factor authentication, backup codes, recovery methods, and connected-account management.

### Release 3

Passkeys, advanced privacy controls, device trust, and optional security-health guidance.

## Important implementation guardrails

- Require re-authentication before changing the password, email, MFA, passkeys, recovery methods, or deleting the account.
- Use server-side rate limits, audit logs, session revocation, and secure token expiration for every security flow.
- Do not rely on email OTP as the only high-assurance factor; authenticator apps and passkeys are stronger options.
- Make every security action accessible, keyboard-friendly, readable, and understandable without security jargon.
- Give users a clear recovery path, but do not weaken verification merely because they lost access to a device.
- Use neutral messages in unauthenticated recovery flows so account existence is not disclosed.

## Final recommendation

Start with a compact Security page organized around Sign-in, Devices, Recovery, and Account controls. The highest-value first additions are Change password, Active sessions, Sign out of all devices, Login activity, Security notifications, and Two-factor authentication. Together, they cover the everyday needs of email-login users and create a strong foundation for passkeys and more advanced account protection later.
