package com.topjohnwu.magisk.ui.superuser

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import io.seventyfivef.executeshellcommand.ui.theme.ExecuteShellCommandTheme
import java.io.DataOutputStream

import com.topjohnwu.magisk.R
import com.topjohnwu.magisk.arch.BaseFragment
import com.topjohnwu.magisk.arch.viewModel
import com.topjohnwu.magisk.databinding.FragmentSuperuserMd2Binding
import org.jetbrains.kotlin.serialization.js.ast.JsAstProtoBuf.Fragment
import rikka.recyclerview.addEdgeSpacing
import rikka.recyclerview.addItemSpacing
import rikka.recyclerview.fixEdgeEffect
import com.topjohnwu.magisk.core.R as CoreR

class TerminalFragment : Fragment() {

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {

            return ComposeView(requireContext()).apply {
                setContent {
                    ExecuteShellCommandTheme {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            ShellCommandUI()
                        }
                    }
                }
            }
        }

    override fun onStart() {
        super.onStart()
        activity?.setTitle("Terminal")
    }


        @Composable
        fun ShellCommandUI() {
            val predefinedCommands = getListOfPredefinedCommands()
            var selectedCommand by remember { mutableStateOf(predefinedCommands[0].second) }
            var customCommand by remember { mutableStateOf(TextFieldValue("")) }
            var output by remember { mutableStateOf("Output will appear here...") }
            var showConfirmDialog by remember { mutableStateOf(false) }

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .padding(top = 20.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row {
                    Text(
                        "Select a Predefined Command:",
                        modifier = Modifier.padding(end = 8.dp),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    var expanded by remember { mutableStateOf(false) }

                    Box {
                        Row(
                            modifier = Modifier.background(Color.LightGray),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                selectedCommand,
                                modifier = Modifier
                                    .height(35.dp)
                                    .wrapContentWidth()
                                    .clickable { expanded = true },
                                fontSize = 20.sp,
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                                overflow = TextOverflow.Ellipsis
                            )

                            Image(
                                painter = painterResource(id = R.drawable.baseline__arrow_down),
                                contentDescription = "Dropdown Icon",
                                modifier = Modifier
                                    .size(30.dp)
                                    .clickable { expanded = true }
                            )
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            predefinedCommands.forEach { cmd ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            cmd.second,
                                            modifier = Modifier.padding(top = 8.dp),
                                            fontSize = 18.sp
                                        )
                                    },
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = 400.dp)
                                        .padding(4.dp),
                                    onClick = {
                                        selectedCommand = cmd.second
                                        customCommand = TextFieldValue(cmd.first)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Text(
                    "Or enter your custom command:",
                    modifier = Modifier.padding(top = 8.dp),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                TextField(
                    value = customCommand,
                    modifier = Modifier.fillMaxWidth(),
                    onValueChange = { customCommand = it },
                    placeholder = {
                        Text(
                            text = "Enter command here...",
                            fontSize = 20.sp,
                            color = Color.Gray
                        )
                    },
                    textStyle = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal
                    )
                )

                Button(
                    onClick = {
                        if (customCommand.text.trim().contains("reboot") ||
                            customCommand.text.trim().contains("rm") ||
                            customCommand.text.trim().contains("rm -rf")
                        ) {
                            showConfirmDialog = true
                        } else {
                            executeShellCommand(customCommand.text) {
                                output = it
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Execute", fontSize = 20.sp)
                }

                HorizontalDivider(color = Color.LightGray)

                Text(
                    "Result:",
                    modifier = Modifier.padding(top = 8.dp),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = output,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .weight(1f)
                        .padding(8.dp),
                    fontSize = 18.sp
                )

                if (showConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showConfirmDialog = false },
                        title = { Text("Confirmation") },
                        text = {
                            Text(
                                "Are you sure you want to execute this command?\n\n\"${customCommand.text}\"",
                                fontSize = 18.sp
                            )
                        },
                        confirmButton = {
                            Button(onClick = {
                                showConfirmDialog = false
                                executeShellCommand(customCommand.text) {
                                    output = it
                                }
                            }) {
                                Text("Yes, Execute")
                            }
                        },
                        dismissButton = {
                            Button(onClick = { showConfirmDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }

        fun getListOfPredefinedCommands(): List<Pair<String, String>> {
            val remountCommand = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                "remount"
            } else {
                "mount -o remount,rw /system"
            }

            return listOf(
                "" to "Select a command",
                remountCommand to "Remount tablet",
                "ls /system/priv-app" to "Display system apps",
                "pm uninstall io.seventyfivef.bacapp" to "Uninstall BacApp (Above 3.2.12)",
                "pm uninstall com.example.bacapp" to "Uninstall BacApp (Below 3.2.13)",
                "rm -rf /system/priv-app/BACapp" to "Remove System App",
                "pm install -r -d -g /sdcard/Download/BACApp_qa_3.2.18.apk" to "Install BacApp",
                "reboot" to "Reboot the device",
                "logcat -d > /sdcard/Download/logFile.txt" to "Save log"
            )
        }

        fun executeShellCommand(command: String, onResult: (String) -> Unit) {
            Thread {
                try {
                    val process = Runtime.getRuntime().exec("su")
                    val os = DataOutputStream(process.outputStream)
                    val stdout = process.inputStream
                    val stderr = process.errorStream

                    os.writeBytes("$command\n")
                    if (command.contains("logcat")) {
                        os.writeBytes("sleep 300\n")
                        onResult("Logcat command executed. Check logFile.txt in Downloads.")
                    }
                    os.writeBytes("exit\n")
                    os.flush()
                    os.close()

                    val stdOutBuffer = stdout.bufferedReader().readText()
                    val errOutBuffer = stderr.bufferedReader().readText()

                    process.waitFor()

                    val result = buildString {
                        if (stdOutBuffer.isNotEmpty()) append("Output:\n$stdOutBuffer")
                        if (errOutBuffer.isNotEmpty()) append("\nERR: $errOutBuffer")
                    }

                    onResult(result)
                } catch (e: Exception) {
                    onResult("Exception: ${e.message}")
                }
            }.start()
        }
    }
