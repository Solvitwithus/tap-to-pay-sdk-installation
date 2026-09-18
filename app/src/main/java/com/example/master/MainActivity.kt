package com.example.master

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.master.ui.theme.MasterTheme
import io.mpos.taptophone.EnrollResultIntent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MasterTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TapToPhoneEnrollmentScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun TapToPhoneEnrollmentScreen(modifier: Modifier = Modifier) {
    val activity = LocalContext.current as ComponentActivity
    var isEnrolled by remember { mutableStateOf(TapToPhoneManager.mposUi.tapToPhone.isDeviceEnrolled()) }
    var lastSerialNumber by remember { mutableStateOf<String?>(null) }

    val enrollLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val isEnrollmentSuccessful = result.resultCode == EnrollResultIntent.ENROLLMENT_RESULT_CODE &&
            data?.getStringExtra(EnrollResultIntent.ENROLLMENT_RESULT_EXTRA) ==
                EnrollResultIntent.ENROLLMENT_RESULT_EXTRA_ENROLLED

        if (isEnrollmentSuccessful) {
            lastSerialNumber = data?.getStringExtra(EnrollResultIntent.ENROLLMENT_RESULT_EXTRA_SERIAL_NUMBER)
            isEnrolled = TapToPhoneManager.mposUi.tapToPhone.isDeviceEnrolled()
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = if (isEnrolled) "Device enrolled" else "Device not enrolled")
        lastSerialNumber?.let { Text(text = "Serial number: $it") }
        Button(onClick = {
            enrollLauncher.launch(TapToPhoneManager.mposUi.tapToPhone.getEnrollDeviceIntent(activity))
        }) {
            Text("Enroll device")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TapToPhoneEnrollmentScreenPreview() {
    MasterTheme {
        Text("Preview unavailable: requires a live MposUi instance")
    }
}