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
import androidx.compose.ui.text.style.TextAlign
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
                    color = Color.Black
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
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text(
                text = "Precisamos das permissões de Câmera e GPS.\nPor favor, aceite para continuar.",
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun CameraAndSpeedScreen() {
    val context = LocalContext.current
    var speedKmH by remember { mutableIntStateOf(0) }
    var hasGpsSignal by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf("") }

    // Gerenciador do GPS
    DisposableEffect(Unit) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()
        
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                hasGpsSignal = true // Detectou satélite
                for (location in result.locations) {
                    if (location.hasSpeed()) {
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

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        
        // Câmera nativa robusta
        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        var camera: Camera? = null
                        
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            try {
                                // Força a abertura da câmera 0 (Traseira Principal)
                                camera = Camera.open(0)
                                camera?.setDisplayOrientation(0)
                                
                                // Ajusta os parâmetros para evitar travamento
                                val params = camera?.parameters
                                if (params != null) {
                                    val focusModes = params.supportedFocusModes
                                    if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                                        params.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
                                    }
                                    camera?.parameters = params
                                }
                                
                                camera?.setPreviewDisplay(holder)
                                camera?.startPreview()
                                cameraError = "" // Limpa o erro se funcionou
                            } catch (e: Exception) {
                                cameraError = "Erro na câmera: ${e.message}"
                                e.printStackTrace()
                            }
                        }
                        
                        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
                        
                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            try {
                                camera?.stopPreview()
                                camera?.release()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    })
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Se a câmera falhar, exibe o erro na tela para podermos debugar
        if (cameraError.isNotEmpty()) {
            Text(
                text = cameraError,
                color = Color.Red,
                modifier = Modifier.align(Alignment.Center).background(Color.Black.copy(alpha = 0.7f)).padding(16.dp)
            )
        }

        // Layout de Velocidade e Status do GPS
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (hasGpsSignal) "$speedKmH km/h" else "Buscando GPS...",
                color = if (hasGpsSignal) Color.White else Color.Yellow,
                fontSize = if (hasGpsSignal) 48.sp else 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
