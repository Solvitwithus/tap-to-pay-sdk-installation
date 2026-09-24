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

    private var merchantId = "visaacceptancedev_t2p"
//    or should i use Organization ID : ke_ttptest as merchantid
    private var merchantSecret = "5McFq/Y5veoP92P/OprWm0z+UDrBquani+pEZ8lLdiY="
    val isConfigured: Boolean get() = merchantId.isNotBlank() && merchantSecret.isNotBlank()

    // Sandbox credentials stay in memory; never persist or compile secrets into the APK.
    fun configureTestCredentials(id: String, secret: String) {
        check(!isConfigured) { "Restart the app to change merchant credentials." }
        require(id.isNotBlank() && secret.isNotBlank()) { "Enter merchant ID and secret key." }
        merchantId = id.trim()
        merchantSecret = secret.trim()
    }

    val mposUi: MposUi by lazy {
        check(isConfigured) { "Configure test merchant credentials first." }
        MposUi.create(
            providerMode = ProviderMode.TEST,
            merchantId = merchantId,
            merchantSecret = merchantSecret,
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
