package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.CompanyProfileEntity

@Composable
fun CompanyLogoImage(
    profile: CompanyProfileEntity?,
    modifier: Modifier = Modifier.size(56.dp)
) {
    val logoUri = profile?.logoUri
    val logoResource = profile?.logoResourceName ?: "img_neo_logo_1790575625683"

    if (!logoUri.isNullOrBlank()) {
        AsyncImage(
            model = Uri.parse(logoUri),
            contentDescription = "Company Logo",
            modifier = modifier.clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
    } else if (logoResource == "ic_launcher_foreground") {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Company Logo",
                modifier = Modifier.size(48.dp)
            )
        }
    } else {
        Image(
            painter = painterResource(id = R.drawable.ic_neo_launcher_3d_1790681547879),
            contentDescription = "Company Logo",
            modifier = modifier.clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
    }
}
