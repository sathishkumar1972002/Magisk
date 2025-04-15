package com.topjohnwu.magisk.ui.terminal

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
import com.topjohnwu.magisk.databinding.FragmentTerminalMd2Binding
import rikka.recyclerview.addEdgeSpacing
import rikka.recyclerview.addItemSpacing
import rikka.recyclerview.fixEdgeEffect
import com.topjohnwu.magisk.core.R as CoreR

class TerminalFragment : BaseFragment<FragmentTerminalMd2Binding>() {

    override val layoutRes: Int
        get() = R.layout.fragment_terminal_md2 // Use the correct layout

    override val viewModel by viewModel() // or use appropriate VM or remove if not needed

    override fun onPreBind(binding: FragmentTerminalMd2Binding) {
        // Use binding.* instead of findViewById
        val commandList = listOf(
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

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            commandList.map { it.second }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.commandSpinner.adapter = adapter
        binding.commandSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                binding.commandInput.setText(commandList[position].first)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.executeButton.setOnClickListener {
            val command = binding.commandInput.text.toString().trim()
            if (command.contains("reboot") || command.contains("rm")) {
                showConfirmationDialog(command, binding)
            } else {
                executeShellCommand(command, binding)
            }
        }
    }

    private fun showConfirmationDialog(command: String, binding: FragmentTerminalMd2Binding) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmation")
            .setMessage("Are you sure you want to execute this command?\n\n\"$command\"")
            .setPositiveButton("Yes, Execute") { _, _ -> executeShellCommand(command, binding) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun executeShellCommand(command: String, binding: FragmentTerminalMd2Binding) {
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
                    binding.outputView.text = if (result.isNotBlank()) result else "No output"
                }

            } catch (e: Exception) {
                activity?.runOnUiThread {
                    binding.outputView.text = "Exception: ${e.message}"
                }
            }
        }.start()
    }

    override fun onStart() {
        super.onStart()
        activity?.title = "Terminal"
    }
}

