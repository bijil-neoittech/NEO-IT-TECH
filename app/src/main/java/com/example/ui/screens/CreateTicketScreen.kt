package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.viewmodel.ServiceViewModel
import com.google.android.gms.location.LocationServices
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTicketScreen(
    viewModel: ServiceViewModel,
    onTicketCreated: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val companyProfile by viewModel.companyProfile.collectAsState()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var customerEmail by remember { mutableStateOf("") }
    // Service Type: strictly "Onsite" or "Workshop"
    var serviceType by remember { mutableStateOf("Onsite") }
    var deviceName by remember { mutableStateOf("") }
    var issueDescription by remember { mutableStateOf("") }
    var locationAddress by remember { mutableStateOf("") }
    var selectedLat by remember { mutableStateOf(12.9716) }
    var selectedLng by remember { mutableStateOf(77.5946) }
    var isDetectingLocation by remember { mutableStateOf(false) }

    // Helper to resolve address from coordinates
    fun reverseGeocodeLocation(lat: Double, lng: Double) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(lat, lng, 1) { addresses ->
                    if (addresses.isNotEmpty()) {
                        val addr = addresses[0]
                        val thoroughfare = addr.thoroughfare ?: addr.subLocality ?: ""
                        val locality = addr.locality ?: ""
                        val admin = addr.adminArea ?: ""
                        val postal = addr.postalCode ?: ""
                        val full = listOf(thoroughfare, locality, admin, postal).filter { it.isNotBlank() }.joinToString(", ")
                        if (full.isNotBlank()) {
                            locationAddress = full
                        }
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val thoroughfare = addr.thoroughfare ?: addr.subLocality ?: ""
                    val locality = addr.locality ?: ""
                    val admin = addr.adminArea ?: ""
                    val postal = addr.postalCode ?: ""
                    val full = listOf(thoroughfare, locality, admin, postal).filter { it.isNotBlank() }.joinToString(", ")
                    if (full.isNotBlank()) {
                        locationAddress = full
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Location fetching function
    fun fetchCurrentDeviceLocation() {
        isDetectingLocation = true
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                isDetectingLocation = false
                if (loc != null) {
                    selectedLat = loc.latitude
                    selectedLng = loc.longitude
                    reverseGeocodeLocation(loc.latitude, loc.longitude)
                    Toast.makeText(context, "Location set: %.4f, %.4f".format(loc.latitude, loc.longitude), Toast.LENGTH_SHORT).show()
                } else {
                    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                    val lastKnown = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    if (lastKnown != null) {
                        selectedLat = lastKnown.latitude
                        selectedLng = lastKnown.longitude
                        reverseGeocodeLocation(lastKnown.latitude, lastKnown.longitude)
                        Toast.makeText(context, "Location updated from network", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Current coordinates locked", Toast.LENGTH_SHORT).show()
                    }
                }
            }.addOnFailureListener {
                isDetectingLocation = false
                Toast.makeText(context, "Could not fetch GPS", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            isDetectingLocation = false
            Toast.makeText(context, "Location permission required", Toast.LENGTH_SHORT).show()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            fetchCurrentDeviceLocation()
        } else {
            Toast.makeText(context, "GPS permission needed to read current location", Toast.LENGTH_SHORT).show()
        }
    }

    fun clearAllEnteredData() {
        customerName = ""
        customerPhone = ""
        customerEmail = ""
        deviceName = ""
        issueDescription = ""
        locationAddress = ""
        Toast.makeText(context, "All entered data cleared", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Register New Service Job") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { clearAllEnteredData() }) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear All Data",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Clear",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. SERVICE TYPE: ONSITE OR WORKSHOP
            Text(
                text = "Service Type",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Onsite Choice
                OutlinedCard(
                    onClick = { serviceType = "Onsite" },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (serviceType == "Onsite") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = if (serviceType == "Onsite") 2.dp else 1.dp,
                        color = if (serviceType == "Onsite") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = if (serviceType == "Onsite") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Onsite",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = if (serviceType == "Onsite") FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (serviceType == "Onsite") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Workshop Choice
                OutlinedCard(
                    onClick = {
                        serviceType = "Workshop"
                        // When workshop is chosen, populate default service center address if available
                        if (companyProfile != null && companyProfile!!.address.isNotBlank()) {
                            locationAddress = companyProfile!!.address
                            selectedLat = companyProfile!!.serviceCenterLat
                            selectedLng = companyProfile!!.serviceCenterLng
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (serviceType == "Workshop") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = if (serviceType == "Workshop") 2.dp else 1.dp,
                        color = if (serviceType == "Workshop") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = if (serviceType == "Workshop") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Workshop",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = if (serviceType == "Workshop") FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (serviceType == "Workshop") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // 2. CUSTOMER INFORMATION
            Text(
                text = "Customer Details",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = customerName,
                onValueChange = { customerName = it },
                label = { Text("Customer Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = customerPhone,
                onValueChange = { customerPhone = it },
                label = { Text("Mobile Number") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = customerEmail,
                onValueChange = { customerEmail = it },
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // 3. DEVICE & ISSUE
            Text(
                text = "Device & Issue",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = deviceName,
                onValueChange = { deviceName = it },
                label = { Text("Device Brand & Model") },
                leadingIcon = { Icon(Icons.Default.Devices, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = issueDescription,
                onValueChange = { issueDescription = it },
                label = { Text("Problem Description") },
                leadingIcon = { Icon(Icons.Default.ReportProblem, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = RoundedCornerShape(12.dp)
            )

            // 4. SERVICE ADDRESS WITH GOOGLE MAP ICON ONLY
            Text(
                text = if (serviceType == "Onsite") "Site Address" else "Workshop / Drop-off Location",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = locationAddress,
                onValueChange = { locationAddress = it },
                label = { Text(if (serviceType == "Onsite") "Customer Address" else "Service Center Location") },
                leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                // GOOGLE MAP ICON ONLY: Tapping detects GPS location or launches Google Maps
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isDetectingLocation) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(
                                onClick = {
                                    // 1. Detect device location if permission granted, otherwise request
                                    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    if (hasFine || hasCoarse) {
                                        fetchCurrentDeviceLocation()
                                    } else {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = "Google Map Location",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ACTIONS ROW: CLEAR DATA & REGISTER BUTTON
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { clearAllEnteredData() },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear", style = MaterialTheme.typography.labelLarge)
                }

                Button(
                    onClick = {
                        if (customerName.isBlank() || customerPhone.isBlank() || deviceName.isBlank()) {
                            Toast.makeText(context, "Please fill required customer and device fields", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.createTicket(
                            customerName = customerName,
                            customerPhone = customerPhone,
                            customerEmail = customerEmail,
                            serviceType = serviceType,
                            deviceName = deviceName,
                            issueDescription = issueDescription,
                            locationAddress = locationAddress,
                            latitude = selectedLat,
                            longitude = selectedLng,
                            onSuccess = {
                                onTicketCreated()
                            }
                        )
                    },
                    modifier = Modifier
                        .weight(2f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Register Job", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}
