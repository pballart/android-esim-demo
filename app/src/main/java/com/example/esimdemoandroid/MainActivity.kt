package com.example.esimdemoandroid

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.os.Bundle
import android.telephony.euicc.DownloadableSubscription
import android.telephony.euicc.EuiccManager
import android.telephony.euicc.EuiccManager.*
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.esimdemoandroid.ui.theme.ESIMDemoAndroidTheme

/**
 * The format of the activation code is "1$<SM-DP+ server URL>$<matchingID>".
 * Can be "LPA:1$<SM-DP+ server URL>$<matchingID>" as well.
 */
private const val TEST_SIM_PROFILE = "REPLACE_ME"

private const val DOWNLOAD_ACTION = "download_subscription"
private const val START_RESOLUTION_ACTION = "start_resolution_action"
private const val BROADCAST_PERMISSION = "com.your.company.lpa.permission.BROADCAST"

class MainActivity : ComponentActivity() {

    private var manager: EuiccManager? = null

    private val eSimBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("esim", "resultCode: $resultCode")
            when {
                /**
                 * Check if same action is triggered again after the allow permission dialog.
                 * if not will throw IntentSender.SendIntentException again fails to be mentioned clearly on android docs
                 */
                DOWNLOAD_ACTION != intent?.action -> return

                resultCode == EMBEDDED_SUBSCRIPTION_RESULT_OK -> showToastMessage("Successfully installed eSIM")

                resultCode == EMBEDDED_SUBSCRIPTION_RESULT_RESOLVABLE_ERROR -> {
                    // Recoverable might be permission issue system will show dialog to allow or deny
                    val startIntent = Intent(START_RESOLUTION_ACTION)
                    val callbackIntent = PendingIntent.getBroadcast(
                        context,
                        0 /* requestCode */,
                        startIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_NO_CREATE
                    )
                    try {
                        manager?.startResolutionActivity(
                            this@MainActivity,
                            0 /* requestCode */,
                            intent,
                            callbackIntent
                        )
                    } catch (e: IntentSender.SendIntentException) {
                        Log.d("esim", e.message.toString())
                    }
                }
            }
        }
    }

    private val resolutionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (START_RESOLUTION_ACTION != intent?.action) {
                return
            }

            val errorCode = intent.getIntExtra(
                EXTRA_EMBEDDED_SUBSCRIPTION_ERROR_CODE,
                0 /* defaultValue*/
            )

            val operationCode =
                intent.getIntExtra(
                    EXTRA_EMBEDDED_SUBSCRIPTION_OPERATION_CODE,
                    0 /* defaultValue*/
                )

            val detailedCode =
                intent.getIntExtra(
                    EXTRA_EMBEDDED_SUBSCRIPTION_DETAILED_CODE,
                    0 /* defaultValue*/
                )

            val subjectCode =
                intent.getIntExtra(
                    EXTRA_EMBEDDED_SUBSCRIPTION_SMDX_SUBJECT_CODE,
                    0 /* defaultValue*/
                )

            val reasonCode =
                intent.getIntExtra(
                    EXTRA_EMBEDDED_SUBSCRIPTION_SMDX_REASON_CODE,
                    0 /* defaultValue*/
                )

            // Helps for debugging if there is an issue with the e-sim
            Log.d("esim", "Result Code: $resultCode Error Code: $errorCode")
            Log.d("esim", "Operation Code: $operationCode Detailed Code: $detailedCode")
            Log.d("esim", "Subject Code: $subjectCode Reason Code: $reasonCode")

            when (resultCode) {
                EMBEDDED_SUBSCRIPTION_RESULT_OK -> showToastMessage("Successfully installed eSIM")
                else -> showToastMessage("Failed to install eSIM ")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ESIMDemoAndroidTheme {
                MainView {
                    //downloadTestProfile()
                    openInstallFlow()
                }
            }
        }
        manager = getSystemService(EUICC_SERVICE) as EuiccManager
    }


    /**
     * Register the broadcast receivers
     */
    override fun onStart() {
        super.onStart()
        registerReceiver(
            eSimBroadcastReceiver,
            IntentFilter(DOWNLOAD_ACTION),
            BROADCAST_PERMISSION,
            null,
            RECEIVER_NOT_EXPORTED
        )
        registerReceiver(
            resolutionReceiver,
            IntentFilter(START_RESOLUTION_ACTION),
            BROADCAST_PERMISSION,
            null,
            RECEIVER_NOT_EXPORTED
        )
    }

    /**
     * Un-Register the broadcast receivers
     */
    override fun onStop() {
        super.onStop()
        unregisterReceiver(eSimBroadcastReceiver)
        unregisterReceiver(resolutionReceiver)
    }

    private fun downloadTestProfile() {
        // Check if e-sim is supported by the device note that emulators are not supported.
        manager?.let {
            if (it.isEnabled) {
                val info = it.euiccInfo
                val osVer = info?.osVersion

                Log.d("esim", "osVer $osVer" )

                val subscription =
                    DownloadableSubscription.forActivationCode(TEST_SIM_PROFILE)
                val callbackIntent = PendingIntent.getBroadcast(
                    baseContext,
                    0 /* requestCode */,
                    Intent(DOWNLOAD_ACTION),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_NO_CREATE
                )
                it.downloadSubscription(subscription, true, callbackIntent)
            } else {
                showToastMessage("eSIM is not supported on this device")
            }
        } ?: showToastMessage("eSIM is not supported on this device")
    }

    private fun openInstallFlow() {
        try {
            val intent = Intent(ACTION_START_EUICC_ACTIVATION)
            intent.putExtra(EXTRA_USE_QR_SCANNER, false)
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace();
            Toast.makeText(this,e.message,Toast.LENGTH_SHORT).show();
        }
    }

    private fun showToastMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}