# Tap to Phone sandbox app

Native Android integration with the Cybersource / Visa Acceptance Devices SDK 2.117.0.

## Run

1. Build with `./gradlew :app:assembleDebug` and install on a supported physical NFC Android device.
2. Obtain an Acceptance Devices **test** merchant ID and secret key, and install/configure the Tap to Pay Ready app as described in the provider onboarding guide.
3. Enter credentials in the app and configure the test merchant. Credentials remain in process memory, are not saved across process restarts, and are not included in the APK.
4. After installing the app, switch off Android Settings > Developer options, then grant the phone permission and enroll the device using the SDK enrollment screen. The SDK rejects certificate generation with `DEVELOPER_MODE_ENABLED` when Developer options are on, including in the test environment. Switching them off disconnects ADB.
5. Enter an amount, supported currency, and sale reference, then select **Take payment**. CDF (Congolese franc) is the default currency; it must be enabled for your merchant.
6. The returned transaction ID populates the management form. Use **Check status**, or enter an original transaction ID to refund it. A blank refund amount requests a full refund; partial refunds require the original currency. Confirm the refund before launching the SDK.

## Behavior and limits

- Uses `ProviderMode.TEST` exclusively. Production credential provisioning and rollout are not implemented.
- Approval is reported only for the SDK approved result. Cancellation or missing results are not treated as proof that a payment failed. Reconcile uncertain outcomes before retrying; the sale reference is not an idempotency guarantee.
- Form and result state survive activity recreation. There is no durable transaction ledger or backend reconciliation. Record transaction IDs externally; if no ID is returned, reconcile in the merchant system.
- Sale (authorization + capture), referenced full/partial refunds, and status lookup are implemented. Other services in the guide, such as pre-authorization and standalone credit, have no dedicated app workflow.
- SDK enrollment and payment screens handle the contactless interaction. Emulator builds do not validate card acceptance. End-to-end verification requires provider onboarding, credentials, and a supported device.

## On-screen SDK logs

The SDK output log below the payment form shows the latest 30 callback/error events, newest first, without ADB or Developer options. It includes timestamps, sale/refund result codes and returned transaction IDs, status lookup results, and enrollment outcomes. Text can be selected and copied; Clear logs empties the history. History survives activity recreation through saved state but is not a durable record across fresh launches. These are SDK outputs and local integration errors, not raw gateway HTTP responses. Credentials, card details, and complete SDK objects are not logged.

## Validation

`./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug`

Unit tests cover decimal precision, invalid amounts/currencies, required references, and sale/refund request construction. On a configured device also check approval, decline, cancellation, enrollment failure, activity recreation, full/partial refunds, and status lookup.

## Provider documentation

- [Payment services](https://developer.cybersource.com/docs/cybs/en-us/tap-to-phone/integration/all/rest/tap-to-phone/tap-to-phone-payment-txn-intro.html)
- [Getting started](https://developer.cybersource.com/docs/cybs/en-us/tap-to-phone/integration/all/rest/tap-to-phone/tap-to-phone-get-started-intro.html)
