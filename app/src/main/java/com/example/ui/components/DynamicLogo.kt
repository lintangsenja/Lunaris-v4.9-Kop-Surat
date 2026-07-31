package com.example.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.R
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File

@Composable
fun DynamicLogo(
    modifier: Modifier = Modifier,
    logoPath: String? = null,
    defaultIcon: ImageVector = Icons.Default.Business,
    defaultIconTint: Color = Color(0xFF7C3AED),
    contentDescription: String? = "Logo"
) {
    val context = LocalContext.current
    var firestoreLogoUrl by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        var listener: com.google.firebase.firestore.ListenerRegistration? = null
        try {
            val firestore = com.example.data.network.FirebaseManager.getFirestore()
            listener = firestore?.collection("pengaturan_global")
                ?.document("profil_admin")
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("DynamicLogo", "Snapshot listener failed", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val url = snapshot.getString("logo_url")
                        firestoreLogoUrl = if (url.isNullOrBlank()) null else url
                    } else {
                        firestoreLogoUrl = null
                    }
                }
        } catch (e: Exception) {
            Log.e("DynamicLogo", "Firestore listener init error", e)
        }

        onDispose {
            listener?.remove()
        }
    }

    val activePath = remember(logoPath, firestoreLogoUrl) {
        if (!logoPath.isNullOrBlank()) logoPath else firestoreLogoUrl
    }

    val decodedBitmap = remember(activePath) {
        if (!activePath.isNullOrEmpty()) {
            try {
                if (activePath.startsWith("data:image/") || activePath.contains("base64,")) {
                    val base64Data = activePath.substringAfter("base64,")
                    val decodedBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)?.asImageBitmap()
                } else if (activePath.startsWith("content://")) {
                    val uri = Uri.parse(activePath)
                    val inputStream = context.contentResolver.openInputStream(uri)
                    BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
                } else if (activePath.startsWith("file://") || activePath.startsWith("/")) {
                    val filePath = if (activePath.startsWith("file://")) Uri.parse(activePath).path ?: "" else activePath
                    if (filePath.isNotEmpty()) {
                        val file = File(filePath)
                        if (file.exists() && file.isFile) {
                            BitmapFactory.decodeFile(filePath)?.asImageBitmap()
                        } else null
                    } else null
                } else if (activePath.length > 200 && !activePath.startsWith("http")) {
                    val decodedBytes = android.util.Base64.decode(activePath, android.util.Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)?.asImageBitmap()
                } else null
            } catch (e: Exception) {
                Log.e("DynamicLogo", "Error decoding activePath in DynamicLogo", e)
                null
            }
        } else null
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (decodedBitmap != null) {
            Image(
                bitmap = decodedBitmap,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (!activePath.isNullOrEmpty() && (activePath.startsWith("http://") || activePath.startsWith("https://"))) {
            AsyncImage(
                model = activePath,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Local PNG logo fallback
            Image(
                painter = painterResource(id = R.drawable.lunaris_app_icon_1784737499587),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
