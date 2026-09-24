package com.example.master

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.master.ui.theme.MasterTheme
import io.mpos.paybutton.MposUi
import io.mpos.taptophone.EnrollResultIntent
import java.util.UUID
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MasterTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    TapToPhoneScreen(this@MainActivity, Modifier.padding(padding))
                }
            }
        }
    }
}

@Composable
fun TapToPhoneScreen(activity: ComponentActivity, modifier: Modifier = Modifier) {
    var configured by remember { mutableStateOf(TapToPhoneManager.isConfigured) }
    var merchantId by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    var enrolled by remember { mutableStateOf(false) }
    var amount by rememberSaveable { mutableStateOf("") }
    var currency by rememberSaveable { mutableStateOf("KES") }
    var reference by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
    var transactionId by rememberSaveable { mutableStateOf("") }
    var refundAmount by rememberSaveable { mutableStateOf("") }
    var busy by rememberSaveable { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf("") }
    var pendingRefund by remember { mutableStateOf<Intent?>(null) }

    var sdkLog by rememberSaveable { mutableStateOf("") }
    var paymentOperation by rememberSaveable { mutableStateOf("Payment") }

    fun logSdk(event: String) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        // Allowlisted fields only: never dump transaction objects, credentials, or intent extras.
        sdkLog = (listOf("$timestamp  $event") + sdkLog.lineSequence().filter { it.isNotBlank() }.toList())
            .take(30).joinToString("\n")
    }

    fun refreshEnrollment() {
        if (configured) {
            runCatching { TapToPhoneManager.mposUi.tapToPhone.isDeviceEnrolled() }
                .onSuccess { enrolled = it }
                .onFailure { message = "Unable to initialize Tap to Phone. Check merchant setup."; logSdk("Unable to initialize Tap to Phone. Check merchant setup. Error type: ${it.javaClass.simpleName}") }
        }
    }
    LaunchedEffect(configured) {
        if (!configured && busy) {
            busy = false
            message = "The app restarted during an operation. Configure credentials and check transaction status before retrying."
        }
        refreshEnrollment()
    }

    val paymentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        busy = false
        // Preserve the SDK identifier even when the payment fails or its result is uncertain.
        val id = result.data?.getStringExtra(MposUi.RESULT_EXTRA_TRANSACTION_IDENTIFIER)
        val outcome = when (result.resultCode) {
            MposUi.RESULT_CODE_APPROVED -> "APPROVED"
            MposUi.RESULT_CODE_FAILED -> "FAILED (declined, aborted, or failed)"
            else -> "UNCONFIRMED"
        }
        logSdk("$paymentOperation callback: code=${result.resultCode}, outcome=$outcome, transactionId=${id ?: "unavailable"}")
        if (!id.isNullOrBlank()) transactionId = id
        message = when (result.resultCode) {
            MposUi.RESULT_CODE_APPROVED -> "Transaction approved." +
                if (id.isNullOrBlank()) " No identifier returned; reconcile before retrying." else " ID: $id"
            MposUi.RESULT_CODE_FAILED -> "Transaction declined, aborted, or failed. Check status before retrying."
            else -> "Payment closed without a confirmed outcome. Check status before retrying."
        }
        if (result.resultCode == MposUi.RESULT_CODE_APPROVED) reference = UUID.randomUUID().toString()
        refreshEnrollment()
    }
    val summaryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        busy = false
        message = if (result.resultCode == MposUi.RESULT_CODE_SUMMARY_CLOSED && TapToPhoneManager.isConfigured) {
            "Transaction status: ${TapToPhoneManager.mposUi.latestTransaction?.status ?: "unavailable"}"
        } else "Status lookup closed without a confirmed result."
        logSdk("Status callback: code=${result.resultCode}. $message")
    }
    val enrollLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        busy = false
        refreshEnrollment()
        message = if (result.resultCode == EnrollResultIntent.ENROLLMENT_RESULT_CODE &&
            result.data?.getStringExtra(EnrollResultIntent.ENROLLMENT_RESULT_EXTRA) ==
            EnrollResultIntent.ENROLLMENT_RESULT_EXTRA_ENROLLED) "Device enrollment completed."
        else "Device enrollment was cancelled or failed."
        logSdk("Enrollment callback: code=${result.resultCode}, enrolled=$enrolled. $message")
    }
    fun enroll() {
        runCatching {
            val intent = TapToPhoneManager.mposUi.tapToPhone.getEnrollDeviceIntent(activity)
            busy = true
            enrollLauncher.launch(intent)
        }.onFailure { busy = false; message = "Unable to open device enrollment."; logSdk("Unable to open device enrollment. Error type: ${it.javaClass.simpleName}") }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        busy = false
        if (granted) enroll() else message = "Phone permission is required for device enrollment."
    }
    fun launchPayment(intent: Intent, operation: String) {
        paymentOperation = operation
        runCatching { busy = true; paymentLauncher.launch(intent) }
            .onFailure { busy = false; message = "Unable to open payment. No approval received."; logSdk("Unable to open payment. No approval received. Error type: ${it.javaClass.simpleName}") }
    }

    Column(modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Tap to Phone", style = MaterialTheme.typography.headlineMedium)
        Text("Test environment — no live payments")
        if (!configured) {
            Text("Enter your Acceptance Devices test merchant credentials.")
            OutlinedTextField(merchantId, { merchantId = it }, label = { Text("Merchant ID") }, singleLine = true)
            OutlinedTextField(secret, { secret = it }, label = { Text("Secret key") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation())
            Button(enabled = merchantId.isNotBlank() && secret.isNotBlank(), onClick = {
                TapToPhoneManager.configureTestCredentials(merchantId, secret)
                secret = ""
                configured = true
            }) { Text("Configure test merchant") }
        } else {
            Text(if (enrolled) "Device enrolled" else "Device not enrolled")
            Button(enabled = !busy, onClick = {
                if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) enroll()
                else { busy = true; permissionLauncher.launch(Manifest.permission.READ_PHONE_STATE) }
            }) { Text("Enroll device") }
            OutlinedTextField(amount, { amount = it }, enabled = !busy, label = { Text("Sale amount") }, singleLine = true)
            OutlinedTextField(currency, { currency = it }, enabled = !busy, label = { Text("Currency (KES = Kenyan shilling)") }, singleLine = true)
            OutlinedTextField(reference, { reference = it }, enabled = !busy, label = { Text("Sale reference") }, singleLine = true)
            Button(enabled = enrolled && !busy, onClick = {
                runCatching { PaymentRequests.sale(amount, currency, reference) }
                    .onSuccess { parameters ->
                        runCatching { TapToPhoneManager.mposUi.createTransactionIntent(parameters) }
                            .onSuccess { transactionId = ""; launchPayment(it, "Sale") }
                            .onFailure { message = "Unable to prepare sale."; logSdk("Unable to prepare sale. Error type: ${it.javaClass.simpleName}") }
                    }.onFailure { message = it.message ?: "Invalid sale details." }
            }) { Text("Take payment") }
            Text("Manage a transaction", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(transactionId, { transactionId = it }, enabled = !busy, label = { Text("Transaction ID") }, singleLine = true)
            Button(enabled = !busy && transactionId.isNotBlank(), onClick = {
                runCatching {
                    val intent = TapToPhoneManager.mposUi.createTransactionSummaryIntent(transactionId.trim())
                    busy = true
                    summaryLauncher.launch(intent)
                }.onFailure { busy = false; message = "Unable to open transaction status."; logSdk("Unable to open transaction status. Error type: ${it.javaClass.simpleName}") }
            }) { Text("Check status") }
            OutlinedTextField(refundAmount, { refundAmount = it }, enabled = !busy,
                label = { Text("Refund amount (blank for full refund)") }, singleLine = true)
            Text("For a partial refund, use the original transaction currency above.")
            Button(enabled = !busy && transactionId.isNotBlank(), onClick = {
                runCatching { PaymentRequests.refund(transactionId, refundAmount, currency) }
                    .onSuccess { parameters ->
                        runCatching { TapToPhoneManager.mposUi.createTransactionIntent(parameters) }
                            .onSuccess { pendingRefund = it }
                            .onFailure { message = "Unable to prepare refund."; logSdk("Unable to prepare refund. Error type: ${it.javaClass.simpleName}") }
                    }.onFailure { message = it.message ?: "Invalid refund details." }
            }) { Text("Refund") }
        }
        if (busy) CircularProgressIndicator()
        if (message.isNotBlank()) Text(message)
        HorizontalDivider()
        Text("SDK output log", style = MaterialTheme.typography.titleLarge)
        Text("Latest 30 events, newest first. Available without Developer options. Session history only; not a permanent payment record.")
        TextButton(enabled = sdkLog.isNotEmpty(), onClick = { sdkLog = "" }) { Text("Clear logs") }
        SelectionContainer {
            Text(sdkLog.ifEmpty { "No SDK results yet." }, style = MaterialTheme.typography.bodySmall)
        }
    }
    pendingRefund?.let { intent ->
        AlertDialog(onDismissRequest = { pendingRefund = null },
            title = { Text("Confirm refund") },
            text = { Text("Refund ${if (refundAmount.isBlank()) "the full amount" else "$refundAmount $currency"} for transaction $transactionId?") },
            confirmButton = { TextButton(onClick = { pendingRefund = null; launchPayment(intent, "Refund") }) { Text("Confirm refund") } },
            dismissButton = { TextButton(onClick = { pendingRefund = null }) { Text("Cancel") } })
    }
}
