package ro.cnpr.inventar

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ro.cnpr.inventar.print.HprtPrinterManager
import ro.cnpr.inventar.print.PrinterHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PrinterControlScreen()
        }
    }
}
//test
@Composable
fun PrinterControlScreen() {
    val context = LocalContext.current
    val printerManager = HprtPrinterManager.getInstance()
    var printerStatus by remember { mutableStateOf("Disconnected") }
    var printerName by remember { mutableStateOf("HM-A300-0F44") } // Default printer name
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Printer Status: $printerStatus")
        Spacer(modifier = Modifier.height(16.dp))
        Row {
            Button(onClick = {
                coroutineScope.launch {
                    try {
                        printerManager.connect(printerName)
                        printerStatus = "Connected"
                        (context as? ComponentActivity)?.runOnUiThread {
                            Toast.makeText(context, "Connected to printer", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        (context as? ComponentActivity)?.runOnUiThread {
                            Toast.makeText(context, "Failed to connect: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }) {
                Text("Connect")
            }
            Button(onClick = {
                printerManager.disconnect()
                printerStatus = "Disconnected"
                Toast.makeText(context, "Disconnected from printer", Toast.LENGTH_SHORT).show()
            }) {
                Text("Disconnect")
            }
            Button(onClick = { 
                coroutineScope.launch {
                    val success = PrinterHelper.printLabel(
                        context,
                        "12345",
                        "Test Asset",
                        "Test Location"
                    )
                    (context as? ComponentActivity)?.runOnUiThread {
                        if (success) {
                            Toast.makeText(context, "Printing test page...", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Printing failed.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }) {
                Text("Print Test")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = printerName,
            onValueChange = { printerName = it },
            label = { Text("Printer Name") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
