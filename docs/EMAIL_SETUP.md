# Email setup (optional)

Veltrix security and notification emails are sent from **FastAPI on Render**, not Firebase Cloud Functions or the `mail` collection. Email is **disabled by default** until you complete this checklist.

## 1. Choose a provider

Set `EMAIL_PROVIDER` to one of:

- `resend` (recommended for quick start)
- `brevo`
- `sendgrid`
- `smtp`

## 2. Domain and DNS

1. Pick a sending domain (e.g. `mail.yourdomain.com` or root domain).
2. Add the provider’s **SPF**, **DKIM**, and **DMARC** records in DNS.
3. Verify the domain in the provider dashboard.

## 3. Environment variables (Render)

| Variable | Required when |
|----------|----------------|
| `EMAIL_ENABLED` | Set `true` only after DNS verification |
| `EMAIL_FROM` | Verified sender address |
| `EMAIL_PROVIDER` | One of the providers above |
| `RESEND_API_KEY` | `EMAIL_PROVIDER=resend` |
| `BREVO_API_KEY` | `EMAIL_PROVIDER=brevo` |
| `SENDGRID_API_KEY` | `EMAIL_PROVIDER=sendgrid` |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD` | `EMAIL_PROVIDER=smtp` |

Firebase Auth emails (verification, password reset) are unchanged and do not use this layer.

## 4. Test

1. Deploy with `EMAIL_ENABLED=false` and confirm pushes still work.
2. Enable email, trigger a security event (e.g. revoke a session), confirm one message arrives.
3. Confirm users with notification email master off do not receive optional emails.

## 5. Privacy

- Do not put message content or credentials in email bodies beyond generic security copy.
- Keep API keys in Render secrets only; never commit `serviceAccount.json` or provider keys.
