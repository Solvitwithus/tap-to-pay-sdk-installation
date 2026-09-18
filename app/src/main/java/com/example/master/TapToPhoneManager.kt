package com.example.master

import io.mpos.accessories.AccessoryFamily
import io.mpos.accessories.parameters.AccessoryParameters
import io.mpos.paybutton.ConfirmationScreenOption
import io.mpos.paybutton.MposUi
import io.mpos.paybutton.SerialNumberInputMethod
import io.mpos.paybutton.TapToPhoneConfiguration
import io.mpos.paybutton.TapToPhoneConnectionType
import io.mpos.paybutton.UiConfiguration
import io.mpos.provider.ProviderMode

object TapToPhoneManager {

    // TODO: fill in with the merchant ID and secret key from the Cybersource/Visa Acceptance
    // Devices test environment (see "Generating a Secret Key for an Existing Merchant ID").
    // Swap ProviderMode.TEST for ProviderMode.LIVE + production credentials when going live.
    private const val MERCHANT_ID = "" // TODO
    private const val MERCHANT_SECRET = "" // TODO

    val mposUi: MposUi by lazy {
        MposUi.create(
            providerMode = ProviderMode.TEST,
            merchantId = MERCHANT_ID,
            merchantSecret = MERCHANT_SECRET,
            terminalParameters = AccessoryParameters.Builder(AccessoryFamily.TAP_TO_PHONE)
                .integrated()
                .build()
        ).apply {
            configuration = UiConfiguration(
                summaryFeatures = setOf(
                    UiConfiguration.SummaryFeature.REFUND_TRANSACTION,
                    UiConfiguration.SummaryFeature.SEND_RECEIPT_VIA_EMAIL,
                    UiConfiguration.SummaryFeature.CAPTURE_TRANSACTION,
                    UiConfiguration.SummaryFeature.RETRY_TRANSACTION,
                    UiConfiguration.SummaryFeature.INCREMENT_TRANSACTION,
                ),
                // Use this to skip the summary screen:
                // resultDisplayBehavior = UiConfiguration.ResultDisplayBehavior.SKIP_SUMMARY_SCREEN,
                // Use this to set signature capture to print on the paper receipt:
                // signatureCapture = UiConfiguration.SignatureCapture.ON_RECEIPT,
            )

            tapToPhone.tapToPhoneConfiguration = TapToPhoneConfiguration(
                serialNumberInputMethod = SerialNumberInputMethod.DEVICE_LIST,
                confirmationScreenOption = ConfirmationScreenOption.SHOW_WITH_SERIAL_NUMBER,
                connectionType = TapToPhoneConnectionType.FOREGROUND_SERVICE,
            )
        }
    }
}
