package com.trackday.live

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.os.Looper
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.location.*
import com.pedro.common.ConnectChecker
import com.pedro.library.rtmp.RtmpCamera1
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
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
                             permissions[Manifest.permission.RECORD_AUDIO] == true &&
                             permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(permissionsToRequest)
    }

    if (permissionsGranted) {
        LiveCameraAndSpeedScreen()
    } else {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text(
                text = "Precisamos das permissões de Câmera, Microfone e GPS.\nPor favor, aceite para continuar.",
                color = Color.White, modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun LiveCameraAndSpeedScreen() {
    val context = LocalContext.current
    var speedKmH by remember { mutableIntStateOf(0) }
    var hasGpsSignal by remember { mutableStateOf(false) }
    
    var isStreaming by remember { mutableStateOf(false) }
    var rtmpCamera by remember { mutableStateOf<RtmpCamera1?>(null) }
    
    val youtubeStreamKey = "COLE_SUA_CHAVE_AQUI" 
    val youtubeUrl = "rtmp://a.rtmp.youtube.com/live2/$youtubeStreamKey"

    DisposableEffect(Unit) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                hasGpsSignal = true
                for (location in result.locations) {
                    if (location.hasSpeed()) {
                        val rawSpeed = location.speed * 3.6
                        speedKmH = if (rawSpeed < 5.0) 0 else rawSpeed.roundToInt()
                    }
                }
            }
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        onDispose { fusedLocationClient.removeLocationUpdates(locationCallback) }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        
        AndroidView(
            factory = { ctx ->
                val surfaceView = SurfaceView(ctx)
                val connectChecker = object : ConnectChecker {
                    override fun onConnectionStarted(url: String) {}
                    override fun onConnectionSuccess() {
                        (ctx as Activity).runOnUiThread {
                            Toast.makeText(ctx, "LIVE INICIADA!", Toast.LENGTH_SHORT).show()
                            isStreaming = true
                        }
                    }
                    override fun onConnectionFailed(reason: String) {
                        (ctx as Activity).runOnUiThread {
                            Toast.makeText(ctx, "Falha na Live: $reason", Toast.LENGTH_LONG).show()
                            rtmpCamera?.stopStream()
                            isStreaming = false
                        }
                    }
                    override fun onNewBitrate(bitrate: Long) {}
                    override fun onDisconnect() {
                        (ctx as Activity).runOnUiThread {
                            Toast.makeText(ctx, "LIVE ENCERRADA", Toast.LENGTH_SHORT).show()
                            isStreaming = false
                        }
                    }
                    override fun onAuthError() {}
                    override fun onAuthSuccess() {}
                }

                rtmpCamera = RtmpCamera1(surfaceView, connectChecker)
                
                surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        if (rtmpCamera?.prepareAudio() == true && rtmpCamera?.prepareVideo() == true) {
                            rtmpCamera?.startPreview()
                        }
                    }
                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {}
                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        if (rtmpCamera?.isStreaming == true) rtmpCamera?.stopStream()
                        rtmpCamera?.stopPreview()
                    }
                })
                surfaceView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Painel Superior Direito: Mapa Mental do Percurso
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(120.dp, 100.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Pista Atual", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "🗺️ Mapeamento", color = Color.Red, fontSize = 10.sp)
                Spacer(modifier = Modifier.weight(1f))
                Text(text = "Best: 1:45.320", color = Color.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Painel Superior Esquerdo: Posição Atual (Ranking Online)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Column {
                Text(text = "POSIÇÃO", color = Color.Gray, fontSize = 10.sp)
                Text(text = "P3 / 12", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "▲ +0.5s P2", color = Color.Green, fontSize = 12.sp)
            }
        }

        // Botão de Disparo da Live
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 32.dp)
                .size(60.dp)
                .clip(CircleShape)
                .background(if (isStreaming) Color.Gray else Color.Red)
                .clickable {
                    if (!isStreaming) {
                        if (youtubeStreamKey == "COLE_SUA_CHAVE_AQUI") {
                            Toast.makeText(context, "Insira sua Stream Key no código!", Toast.LENGTH_LONG).show()
                        } else {
                            if (rtmpCamera?.prepareAudio() == true && rtmpCamera?.prepareVideo() == true) {
                                rtmpCamera?.startStream(youtubeUrl)
                            } else {
                                Toast.makeText(context, "Erro de Inicialização de Mídia", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        rtmpCamera?.stopStream()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isStreaming) "STOP" else "LIVE",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }

        // Velocímetro Digital Inferior
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .padding(horizontal = 32.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (hasGpsSignal) "$speedKmH" else "--",
                color = if (hasGpsSignal) Color.White else Color.Yellow,
                fontSize = 64.sp,
                fontWeight = FontWeight.Black
            )
            Text(text = "KM/H", color = Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
