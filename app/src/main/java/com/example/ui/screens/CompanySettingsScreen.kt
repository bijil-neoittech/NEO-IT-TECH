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
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.CompanyProfileEntity
import com.example.ui.components.CompanyLogoImage
import com.example.viewmodel.ServiceViewModel
import com.google.android.gms.location.LocationServices
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanySettingsScreen(
    viewModel: ServiceViewModel,
    onBack: () -> Unit
) {
    val companyProfile by viewModel.companyProfile.collectAsState()
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var companyName by remember(companyProfile) { mutableStateOf(companyProfile?.companyName ?: "NEO IT TECH") }
    var tagline by remember(companyProfile) { mutableStateOf(companyProfile?.tagline ?: "Next Gen Computer Solutions & Repairs") }
    var ticketSeriesPrefix by remember(companyProfile) { mutableStateOf(companyProfile?.ticketSeriesPrefix ?: "NEO-2026-") }
    var address by remember(companyProfile) { mutableStateOf(companyProfile?.address ?: "Tech Park, MG Road, Bengaluru, Karnataka - 560001") }
    var gstin by remember(companyProfile) { mutableStateOf(companyProfile?.gstin ?: "29AABCN1234F1Z5") }
    var taxEnabled by remember(companyProfile) { mutableStateOf(companyProfile?.taxEnabled ?: true) }
    var phone by remember(companyProfile) { mutableStateOf(companyProfile?.phone ?: "+91 98765 43210") }
    var email by remember(companyProfile) { mutableStateOf(companyProfile?.email ?: "support@neoservice.in") }
    var terms by remember(companyProfile) { mutableStateOf(companyProfile?.termsAndConditions ?: "") }
    var currency by remember(companyProfile) { mutableStateOf(companyProfile?.currencySymbol ?: "₹") }
    var colorTheme by remember(companyProfile) { mutableStateOf(companyProfile?.colorTheme ?: "NEO_BLUE") }
    var logoResourceName by remember(companyProfile) { mutableStateOf(companyProfile?.logoResourceName ?: "img_neo_logo_1790575625683") }
    var logoUri by remember(companyProfile) { mutableStateOf(companyProfile?.logoUri ?: "") }
    var serviceLat by remember(companyProfile) { mutableStateOf(companyProfile?.serviceCenterLat ?: 12.9716) }
    var serviceLng by remember(companyProfile) { mutableStateOf(companyProfile?.serviceCenterLng ?: 77.5946) }
    var isDetectingLocation by remember { mutableStateOf(false) }

    val colorThemes = listOf("NEO_BLUE", "EMERALD", "PURPLE")

    // Zero-permission Android Photo Picker for custom logo
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            logoUri = uri.toString()
            Toast.makeText(context, "Custom logo selected!", Toast.LENGTH_SHORT).show()
        }
    }

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
                            address = full
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
                        address = full
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // GPS location detection for service center
    fun fetchServiceCenterLocation() {
        isDetectingLocation = true
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                isDetectingLocation = false
                if (loc != null) {
                    serviceLat = loc.latitude
                    serviceLng = loc.longitude
                    reverseGeocodeLocation(loc.latitude, loc.longitude)
                    Toast.makeText(context, "Service Center GPS set: %.4f, %.4f".format(loc.latitude, loc.longitude), Toast.LENGTH_SHORT).show()
                } else {
                    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                    val lastKnown = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    if (lastKnown != null) {
                        serviceLat = lastKnown.latitude
                        serviceLng = lastKnown.longitude
                        reverseGeocodeLocation(lastKnown.latitude, lastKnown.longitude)
                        Toast.makeText(context, "Location retrieved from network provider", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Current location coordinates updated", Toast.LENGTH_SHORT).show()
                    }
                }
            }.addOnFailureListener {
                isDetectingLocation = false
                Toast.makeText(context, "GPS fetch failed: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            isDetectingLocation = false
            Toast.makeText(context, "Location permission required", Toast.LENGTH_SHORT).show()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fine || coarse) {
            fetchServiceCenterLocation()
        } else {
            Toast.makeText(context, "GPS permission needed to set current location", Toast.LENGTH_LONG).show()
        }
    }

    // Temporary profile for previewing logo changes in real-time
    val previewProfile = CompanyProfileEntity(
        companyName = companyName,
        logoResourceName = logoResourceName,
        logoUri = logoUri
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Company Profile & Configuration") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. COMPANY PROFILE LOGO CHANGE OPTIONS
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Company Profile Logo Options",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Choose a corporate logo or pick a custom logo from your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Current Live Logo Preview
                        CompanyLogoImage(
                            profile = previewProfile,
                            modifier = Modifier
                                .size(72.dp)
                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = if (logoUri.isNotBlank()) "Custom Picked Logo" else if (logoResourceName == "ic_launcher_foreground") "Cyber Blue Crest" else "NEO 3D Tech Logo",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Choose Photo")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Preset Logo Styles:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = logoResourceName == "img_neo_logo_1790575625683" && logoUri.isBlank(),
                            onClick = {
                                logoResourceName = "img_neo_logo_1790575625683"
                                logoUri = ""
                            },
                            label = { Text("NEO 3D Metallic") }
                        )

                        FilterChip(
                            selected = logoResourceName == "ic_launcher_foreground" && logoUri.isBlank(),
                            onClick = {
                                logoResourceName = "ic_launcher_foreground"
                                logoUri = ""
                            },
                            label = { Text("Cyber Crest") }
                        )

                        if (logoUri.isNotBlank()) {
                            FilterChip(
                                selected = true,
                                onClick = { logoUri = "" },
                                label = { Text("Reset to Preset") },
                                leadingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            )
                        }
                    }
                }
            }

            // 2. GSTN / TAX ID COLUMNS ENABLE / DISABLE BUTTON
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (taxEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "GSTN / Tax ID Columns",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (taxEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (taxEnabled) "GST enabled: 18% GST (CGST/SGST) & GSTIN will be printed on all invoices & estimates." else "GST disabled: Tax exempt invoices; GSTIN and tax rows are omitted.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Enable / Disable Switch Button
                        Switch(
                            checked = taxEnabled,
                            onCheckedChange = { taxEnabled = it },
                            thumbContent = {
                                Icon(
                                    imageVector = if (taxEnabled) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        )
                    }

                    if (taxEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = gstin,
                            onValueChange = { gstin = it },
                            label = { Text("Company GSTIN / Tax Identification Number") },
                            leadingIcon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // 3. SERVICE CENTER GOOGLE LOCATION SET OPTION
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Service Center Google Location",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        TextButton(
                            onClick = {
                                val mapUri = Uri.parse("geo:$serviceLat,$serviceLng?q=$serviceLat,$serviceLng(${Uri.encode(companyName)})")
                                val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
                                try {
                                    context.startActivity(mapIntent)
                                } catch (e: Exception) {
                                    val browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$serviceLat,$serviceLng")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, browserUri))
                                }
                            }
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open in Maps")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Service Center Official Street Address") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Coordinates: %.4f° N, %.4f° E".format(serviceLat, serviceLng),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Used for customer drop-offs and technician routing",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = {
                                val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                if (hasFine || hasCoarse) {
                                    fetchServiceCenterLocation()
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            if (isDetectingLocation) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Detecting...")
                            } else {
                                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Set Current GPS")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Quick Hub Presets:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("MG Road HQ", 12.9716, 77.5946),
                            Triple("Indiranagar Hub", 12.9784, 77.6408),
                            Triple("Manyata Park", 13.0521, 77.6212),
                            Triple("Whitefield", 12.9698, 77.7500)
                        ).forEach { (name, lat, lng) ->
                            SuggestionChip(
                                onClick = {
                                    address = "$name, Bengaluru, Karnataka"
                                    serviceLat = lat
                                    serviceLng = lng
                                },
                                label = { Text(name, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }

            // General Profile Details & Branding
            Text(
                text = "General Business Details & Branding",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = companyName,
                onValueChange = { companyName = it },
                label = { Text("Company Legal Name") },
                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = tagline,
                onValueChange = { tagline = it },
                label = { Text("Business Tagline / Subtitle") },
                leadingIcon = { Icon(Icons.Default.Subtitles, contentDescription = null) },
                placeholder = { Text("e.g. Next Gen Computer Solutions & Repairs") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Ticket Series Prefix Editing
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Job Ticket Series & Numbering Format",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Customize the prefix used for all newly generated work order tickets.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = ticketSeriesPrefix,
                        onValueChange = { ticketSeriesPrefix = it },
                        label = { Text("Ticket Series Prefix") },
                        placeholder = { Text("e.g. NEO-2026-, NIT-, JOB-") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Helpline Phone") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Support Email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Currency & Appearance
            Text(text = "Currency Symbol", style = MaterialTheme.typography.bodySmall)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("₹", "$", "€", "£", "¥").forEach { curr ->
                    FilterChip(
                        selected = currency == curr,
                        onClick = { currency = curr },
                        label = { Text(curr) }
                    )
                }
            }

            Text(text = "Portal Theme Color", style = MaterialTheme.typography.bodySmall)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                colorThemes.forEach { theme ->
                    FilterChip(
                        selected = colorTheme == theme,
                        onClick = { colorTheme = theme },
                        label = { Text(theme.replace("_", " ")) }
                    )
                }
            }

            OutlinedTextField(
                value = terms,
                onValueChange = { terms = it },
                label = { Text("Invoice Terms & Conditions") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.saveCompanyProfile(
                        CompanyProfileEntity(
                            id = 1,
                            companyName = companyName,
                            tagline = tagline,
                            ticketSeriesPrefix = ticketSeriesPrefix,
                            address = address,
                            gstin = gstin,
                            taxEnabled = taxEnabled,
                            phone = phone,
                            email = email,
                            termsAndConditions = terms,
                            currencySymbol = currency,
                            colorTheme = colorTheme,
                            logoResourceName = logoResourceName,
                            logoUri = logoUri,
                            serviceCenterLat = serviceLat,
                            serviceCenterLng = serviceLng
                        )
                    )
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Company Configuration", style = MaterialTheme.typography.titleMedium)
            }

            OutlinedButton(
                onClick = {
                    viewModel.resetOnboarding()
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Re-launch First Time Setup (\"Let's Get Started\")")
            }
        }
    }
}
