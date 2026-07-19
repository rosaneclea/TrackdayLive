package com.trackday.live

import android.Manifest
import android.annotation.SuppressLint
import android.hardware.Camera
import android.os.Bundle
import android.os.Looper
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.location.*
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TrackDayApp()
                }
            }
        }
    }
}

@Composable
fun TrackDayApp() {
    val permissionsToRequest = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    var permissionsGranted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionsGranted = permissions[Manifest.permission.CAMERA] == true &&
                             permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(permissionsToRequest)
    }

    if (permissionsGranted) {
        CameraAndSpeedScreen()
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Precisamos das permissões de Câmera e GPS para funcionar.")
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun CameraAndSpeedScreen() {
    val context = LocalContext.current
    var speedKmH by remember { mutableIntStateOf(0) }

    // Configuração do GPS para atualizar a cada 1 segundo (1000 ms)
    DisposableEffect(Unit) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()
        
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    if (location.hasSpeed()) {
                        // O Android devolve a velocidade em metros por segundo (m/s)
                        // Multiplicamos por 3.6 para converter para km/h
                        speedKmH = (location.speed * 3.6).roundToInt()
                    }
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    // Interface: Câmera no fundo e Velocidade por cima
    Box(modifier = Modifier.fillMaxSize()) {
        
        // Visualização da Câmera
        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        var camera: Camera? = null
                        
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            try {
                                camera = Camera.open() // Abre a câmera traseira padrão
                                camera?.setDisplayOrientation(0) // Ajusta a rotação se necessário
                                camera?.setPreviewDisplay(holder)
                                camera?.startPreview()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        
                        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
                        
                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            camera?.stopPreview()
                            camera?.release()
                        }
                    })
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Marcador de Velocidade (Estilo TrackDay)
        Text(
            text = "$speedKmH km/h",
            color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .background(Color.Black.copy(alpha = 0.5f)) // Fundo semitransparente para dar contraste
                .padding(horizontal = 24.dp, vertical = 8.dp)
        )
    }
}
