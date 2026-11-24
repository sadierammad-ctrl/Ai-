package com.example.ui.screens

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.animations.BabyAiOrb
import com.example.animations.OrbAnimationState
import com.example.ui.components.GlassCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.wallpaper.BabyLiveWallpaperService

@Composable
fun LiveWallpaperPreviewScreen(
    animationsEnabled: Boolean,
    batterySavingMode: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Text("BABY AI Live Wallpaper", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            }

            GlassCard {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_galaxy_nebula_1795497884886),
                        contentDescription = "BABY AI Galaxy Nebula Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    BabyAiOrb(
                        state = OrbAnimationState.IDLE,
                        animationsEnabled = animationsEnabled,
                        batterySavingMode = batterySavingMode,
                        size = 160.dp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Animated Cosmic Galaxy & Glowing AI Orb",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Battery-efficient rendering pauses automatically when your screen is off or covered. Requires zero microphone permissions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = {
                    val directIntent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                        putExtra(
                            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                            ComponentName(context, BabyLiveWallpaperService::class.java)
                        )
                    }
                    runCatching { context.startActivity(directIntent) }
                        .onFailure {
                            val fallback = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
                            runCatching { context.startActivity(fallback) }
                                .onFailure {
                                    Toast.makeText(context, "Live Wallpaper picker unavailable on this device.", Toast.LENGTH_SHORT).show()
                                }
                        }
                },
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Wallpaper, contentDescription = "Apply Wallpaper")
                Spacer(modifier = Modifier.padding(4.dp))
                Text("Apply Live Wallpaper")
            }
        }
    }
}
