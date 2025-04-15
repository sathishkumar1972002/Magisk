package com.topjohnwu.magisk.ui.superuser

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import java.io.DataOutputStream

import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

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

    private lateinit var spinner: Spinner
    private lateinit var commandInput: EditText
    private lateinit var executeButton: Button
    private lateinit var outputView: TextView

    private val commandList = listOf(
        "" to "Select a command",
        (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "remount" else "mount -o remount,rw /system") to "Remount tablet",
        "ls /system/priv-app" to "Display system apps",
        "pm uninstall io.seventyfivef.bacapp" to "Uninstall BacApp (Above 3.2.12)",
        "pm uninstall com.example.bacapp" to "Uninstall BacApp (Below 3.2.13)",
        "rm -rf /system/priv-app/BACapp" to "Remove System App",
        "pm install -r -d -g /sdcard/Download/BACApp_qa_3.2.18.apk" to "Install BacApp",
        "reboot" to "Reboot the device",
        "logcat -d > /sdcard/Download/logFile.txt" to "Save log"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_terminal, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        spinner = view.findViewById(R.id.commandSpinner)
        commandInput = view.findViewById(R.id.commandInput)
        executeButton = view.findViewById(R.id.executeButton)
        outputView = view.findViewById(R.id.outputView)

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            commandList.map { it.second }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                commandInput.setText(commandList[position].first)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        executeButton.setOnClickListener {
            val command = commandInput.text.toString().trim()
            if (command.contains("reboot") || command.contains("rm")) {
                showConfirmationDialog(command)
            } else {
                executeShellCommand(command)
            }
        }
    }

    private fun showConfirmationDialog(command: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmation")
            .setMessage("Are you sure you want to execute this command?\n\n\"$command\"")
            .setPositiveButton("Yes, Execute") { _, _ -> executeShellCommand(command) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun executeShellCommand(command: String) {
        Thread {
            try {
                val process = Runtime.getRuntime().exec("su")
                val os = DataOutputStream(process.outputStream)
                val stdout = process.inputStream
                val stderr = process.errorStream

                os.writeBytes("$command\n")
                if (command.contains("logcat")) {
                    os.writeBytes("sleep 3\n")
                }
                os.writeBytes("exit\n")
                os.flush()
                os.close()

                val out = stdout.bufferedReader().readText()
                val err = stderr.bufferedReader().readText()
                process.waitFor()

                val result = buildString {
                    if (out.isNotEmpty()) append("Output:\n$out")
                    if (err.isNotEmpty()) append("\nERR:\n$err")
                }

                activity?.runOnUiThread {
                    outputView.text = if (result.isNotBlank()) result else "No output"
                }

            } catch (e: Exception) {
                activity?.runOnUiThread {
                    outputView.text = "Exception: ${e.message}"
                }
            }
        }.start()
    }

    override fun onStart() {
        super.onStart()
        activity?.title = "Terminal"
    }
}

