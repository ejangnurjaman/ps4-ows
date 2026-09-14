package com.truecarbide.ps4ows

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.truecarbide.ps4ows.R
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream

class MainActivity : AppCompatActivity() {

    private var server: Ps4HttpServer? = null
    private var isServerRunning = false
    private val serverPort = 8080
    private var isToolkitReady = false

    // Official SHA-256 Hash for ps4ows.zip
    private val OFFICIAL_HASH = "3024070420f67d102d886cc9e785e36b70144693e50ca9ddee35c8a6dfdc1185"

    private lateinit var tvPort: TextView
    private lateinit var tvNetworkStatus: TextView
    private lateinit var tvServerUrl: TextView
    private lateinit var tvZipPath: TextView
    private lateinit var btnStartStop: Button
    private lateinit var connectivityManager: ConnectivityManager

    private val zipPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleSelectedZip(it) }
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ -> }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            runOnUiThread { updateNetworkInfo() }
        }

        override fun onLost(network: Network) {
            runOnUiThread { updateNetworkInfo() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvPort = findViewById(R.id.tvPort)
        tvNetworkStatus = findViewById(R.id.tvNetworkStatus)
        tvServerUrl = findViewById(R.id.tvServerUrl)
        tvZipPath = findViewById(R.id.tvZipPath)
        btnStartStop = findViewById(R.id.btnStartStop)
        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        findViewById<LinearLayout>(R.id.llZipPicker).setOnClickListener {
            zipPickerLauncher.launch("application/zip")
        }

        btnStartStop.setOnClickListener {
            if (isServerRunning) stopServer() else startServer()
        }

        findViewById<Button>(R.id.btnHowToUse).setOnClickListener { showHowToUseDialog() }
        findViewById<Button>(R.id.btnAbout).setOnClickListener { showAboutDialog() }

        checkToolkitStatus()
        checkPermissions()
        registerNetworkCallback()
        updateNetworkInfo()
        createNotificationChannel()
    }

    private fun checkToolkitStatus() {
        val toolkitDir = File(filesDir, "toolkit")
        isToolkitReady = toolkitDir.exists() && toolkitDir.isDirectory && (toolkitDir.list()?.isNotEmpty() ?: false)
        if (isToolkitReady) {
            tvZipPath.text = "Toolkit is ready"
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val toRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (toRequest.isNotEmpty()) {
            permissionLauncher.launch(toRequest.toTypedArray())
        }
    }

    private fun registerNetworkCallback() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    private fun startServer() {
        if (!isToolkitReady) {
            showErrorDialog("Please select and extract toolkit ZIP first.")
            return
        }

        try {
            server = Ps4HttpServer(this, serverPort)
            server?.start()
            isServerRunning = true
            btnStartStop.text = "Stop Server"
            btnStartStop.setBackgroundResource(R.drawable.bg_stop_btn)
            updateNetworkInfo()
            showNotification("Server Running", "Access at http://${NetworkUtils.getLocalIpAddress()}:$serverPort")
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMessage = if (e.message?.contains("Address already in use") == true) {
                "Failed to start server: Port $serverPort is already in use."
            } else {
                "Error starting server: ${e.message}"
            }
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            showErrorDialog(errorMessage)
        }
    }

    private fun stopServer() {
        server?.stop()
        server = null
        isServerRunning = false
        btnStartStop.text = "Start Server"
        btnStartStop.setBackgroundResource(R.drawable.bg_start_btn)
        updateNetworkInfo()
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(1)
    }

    private fun updateNetworkInfo() {
        val ip = NetworkUtils.getLocalIpAddress()
        tvPort.text = "Port: $serverPort"
        if (ip == "0.0.0.0") {
            tvNetworkStatus.text = "Offline"
            tvNetworkStatus.setTextColor(Color.RED)
        } else {
            tvNetworkStatus.text = "Online"
            tvNetworkStatus.setTextColor(Color.parseColor("#00FF00")) // Lime
        }
        tvServerUrl.text = "URL: http://$ip:$serverPort"
        
        if (isServerRunning) {
            showNotification("Server Running", "Access at http://$ip:$serverPort")
        }
    }

    private fun handleSelectedZip(uri: Uri) {
        val fileName = getFileName(uri)
        if (fileName?.endsWith(".zip") != true) {
            showErrorDialog("Please select a valid .zip file")
            return
        }

        try {
            // 1. Temporary copy to calculate hash
            val tempFile = File(cacheDir, "temp_toolkit.zip")
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            // 2. Verify SHA-256 Hash
            val fileHash = calculateSHA256(tempFile)
            if (fileHash != OFFICIAL_HASH) {
                tempFile.delete()
                showErrorDialog("Verification Failed: Invalid or modified ZIP file detected.\n\nHash: $fileHash")
                return
            }

            // 3. Hash is valid, proceed to extract
            tempFile.inputStream().use { input ->
                extractZip(input)
            }
            tempFile.delete()
            
            isToolkitReady = true
            tvZipPath.text = "Toolkit verified & ready"
            Toast.makeText(this, "Toolkit Verified Successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            showErrorDialog("Failed to process toolkit: ${e.message}")
        }
    }

    private fun calculateSHA256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        file.inputStream().use { input ->
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun extractZip(inputStream: InputStream) {
        val toolkitDir = File(filesDir, "toolkit")
        if (toolkitDir.exists()) toolkitDir.deleteRecursively()
        toolkitDir.mkdirs()

        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val newFile = File(toolkitDir, entry.name)
                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    FileOutputStream(newFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
        return name ?: uri.path?.substringAfterLast('/')
    }

    private fun showHowToUseDialog() {
        val message = """
            1. Prepare Connection: Turn off Mobile Data and external Wi-Fi.
            
            2. Connect: Enable Mobile Hotspot and connect your PS4 to it.
            
            3. Select Toolkit: Tap "Select Toolkit" and pick your ps4ows.zip file. This will extract the files needed for the server.
            
            4. Start Server: Tap the [Start Server] button.
            
            5. Access on PS4:
            ◦ Open the Web Browser on your PS4.
            ◦ Type in the URL provided by the app (e.g., http://192.168.x.x:8080).
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle("How to Use")
            .setMessage(message)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showAboutDialog() {
        val message = """
            PS4 OWS (Offline Webkit Server)
            Created by youtube.com/@truecarbide
            Powered by weebsu.com
            
            v1.0.0-beta
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("About")
            .setMessage(message)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Server Status"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel("server_channel", name, importance).apply {
                description = "Notifications for PS4 OWS Server status"
            }
            val notificationManager: NotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(title: String, content: String) {
        val builder = NotificationCompat.Builder(this, "server_channel")
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, builder.build())
    }

    override fun onDestroy() {
        super.onDestroy()
        connectivityManager.unregisterNetworkCallback(networkCallback)
        stopServer()
    }
}
