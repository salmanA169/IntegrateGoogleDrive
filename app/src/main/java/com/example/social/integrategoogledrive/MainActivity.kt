package com.example.social.integrategoogledrive

import android.os.Bundle
import android.widget.Space
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.social.integrategoogledrive.ui.theme.IntegrateGoogleDriveTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IntegrateGoogleDriveTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel = hiltViewModel<MainViewModel>()
                    val state by viewModel.state.collectAsState()
                    val effect by viewModel.effect.collectAsState()

                    var showDialog by remember {
                        mutableStateOf(false)
                    }
                    val context = LocalContext.current
                    val signInLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartIntentSenderForResult(),
                        onResult = {
                            viewModel.onEvent(
                                MainEvent.OnSignInResult(
                                    it.data ?: return@rememberLauncherForActivityResult
                                )
                            )
                        }
                    )
                    val authorizeLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartIntentSenderForResult(),
                        onResult = {
                            viewModel.onEvent(
                                MainEvent.OnAuthorize(
                                    it.data ?: return@rememberLauncherForActivityResult
                                )
                            )
                        }
                    )

                    val pickerPhotoLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.PickVisualMedia(),
                        onResult = {
                            viewModel.onEvent(
                                MainEvent.Backup(
                                    it ?: return@rememberLauncherForActivityResult
                                )
                            )
                        }
                    )
                    LaunchedEffect(key1 = effect) {
                        when (effect) {
                            is MainEffect.Authorize -> {
                                authorizeLauncher.launch(
                                    IntentSenderRequest.Builder((effect as MainEffect.Authorize).intentSender)
                                        .build()
                                )
                            }

                            is MainEffect.SignIn -> {
                                signInLauncher.launch(
                                    IntentSenderRequest.Builder((effect as MainEffect.SignIn).intentSender)
                                        .build()
                                )
                            }

                            null -> Unit
                        }
                    }
                    if (showDialog) {
                        LaunchedEffect(key1 = true) {
                            viewModel.onEvent(MainEvent.GetFiles)
                        }
                        Dialog(onDismissRequest = { showDialog = false }) {
                            Surface(tonalElevation = 4.dp) {
                                LazyVerticalGrid(columns = GridCells.Fixed(2)) {
                                    items(state.restoreFiles) {
                                        Box(modifier = Modifier
                                            .fillMaxSize()
                                            .clickable {
                                                viewModel.onEvent(MainEvent.Restore(it.id))
                                                Toast
                                                    .makeText(context, "Saved to local storage", Toast.LENGTH_SHORT)
                                                    .show()
                                                showDialog = false
                                            }) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext
                                                    .current).data(it.thumbnailFileLink).placeholder(R.drawable.drive_icon).build(),
                                                contentDescription = "",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.size(150.dp)
                                            )
                                            Text(
                                                text = it.nameFile, modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(
                                                        Brush.linearGradient(
                                                            listOf(Color.Black, Color.Transparent)
                                                        )
                                                    )
                                                    .align(Alignment.BottomCenter)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.drive_icon),
                            modifier = Modifier.size(80.dp),
                            contentDescription = ""
                        )
                        Text(text = "Backup Drive")
                        if (state.email == null) {
                            Button(onClick = { viewModel.onEvent(MainEvent.SignInGoogle) }) {
                                Text(text = "Sign in")
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(onClick = { pickerPhotoLauncher.launch(PickVisualMediaRequest()) }) {
                                    Text(text = "Backup")
                                }
                                Button(onClick = {showDialog= true  }) {
                                    Text(text = "Restore")
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = "Welcome ${state.email}")
                            Button(onClick = { viewModel.onEvent(MainEvent.SignOut) }) {
                                Text(text = "Sign out")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    IntegrateGoogleDriveTheme {
        Greeting("Android")
    }
}