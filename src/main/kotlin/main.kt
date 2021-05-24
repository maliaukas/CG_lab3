import androidx.compose.desktop.AppManager
import androidx.compose.desktop.AppWindow
import androidx.compose.desktop.Window
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import nu.pattern.OpenCV
import org.opencv.core.Mat
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

enum class ButtonType {
    ADD, NEGATE, LOG, POWER, MULTIPLY,
    LINEAR_CONTR,
    ADAPTIVE,
    BERNSEN, SAUVOLA
}

fun main() {
    Window(title = "Lab 3 - CG - Malyavko", size = IntSize(width = 1000, height = 750)) {
        var imagePath by remember { mutableStateOf("") }
        var update by remember { mutableStateOf(true) }
        var modifiedImagePath by remember { mutableStateOf("") }
        val image: MutableState<Mat?> = remember { mutableStateOf(null) }
        var elementWiseValue by remember { mutableStateOf(TextFieldValue("0")) }
        var localMethodRValue by remember { mutableStateOf(TextFieldValue("15")) }

        val defModifier = Modifier.fillMaxWidth().padding(2.dp)

        fun deleteTempFile() {
            if (modifiedImagePath.isNotBlank())
                Files.deleteIfExists(Path.of(modifiedImagePath))
        }

        AppManager.setEvents(onAppExit = { deleteTempFile() })

        OpenCV.loadLocally()

        fun updateImage() {
            update = false
            update = true
        }

        fun handleButtonClick(type: ButtonType) {
            try {
                val img = image.value!!
                val newIm = when (type) {
                    ButtonType.ADD -> add(img, elementWiseValue.text.toDouble())
                    ButtonType.NEGATE -> negate(img)
                    ButtonType.LOG -> log(img)
                    ButtonType.POWER -> power(img, elementWiseValue.text.toDouble())
                    ButtonType.MULTIPLY -> mul(img, elementWiseValue.text.toDouble())
                    ButtonType.LINEAR_CONTR -> linearContrast(img)
                    ButtonType.ADAPTIVE -> adaptive(img, localMethodRValue.text.toInt())
                    ButtonType.BERNSEN, ButtonType.SAUVOLA -> {
                        val r = localMethodRValue.text.toInt()
                        if (r % 2 == 0) {
                            AppWindow(size = IntSize(200, 100)).show {
                                Text("Neighbourhood size must be odd!")
                            }
                            return
                        }
                        localMethod(
                            type = if (type == ButtonType.BERNSEN) LocalMethodType.BERNSEN else LocalMethodType.SAUVOLA,
                            m = img,
                            r = r
                        )
                    }
                }
                saveImage(newIm, modifiedImagePath)
                updateImage()
            } catch (e: NumberFormatException) {
                AppWindow(size = IntSize(200, 100)).show {
                    Text("Wrong value input!")
                }
            }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            val state = rememberScrollState(0f)
            ScrollableColumn(
                scrollState = state,
                modifier = Modifier.fillMaxWidth(0.20f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        deleteTempFile()
                        val path = chooseFile()
                        if (path != null) {
                            image.value = loadImage(path)
                            imagePath = path
                            modifiedImagePath = tempFile(path)
                            saveImage(image.value, modifiedImagePath)
                        }
                    },
                    modifier = defModifier,
                ) { Text(text = "Choose file") }

                Text(imagePath, modifier = defModifier)
                Text("Elementwise operations", modifier = Modifier.padding(2.dp))

                OutlinedTextField(
                    value = elementWiseValue,
                    modifier = defModifier,
                    onValueChange = {
                        val replaced = it.text.replace(Regex("[^0-9.\\-]+"), "")
                        elementWiseValue = if (replaced == it.text) {
                            it.copy(replaced)
                        } else {
                            it.copy(replaced, TextRange(it.selection.end - 1))
                        }
                    },
                    label = { Text("Value to add / multiply / power") },
                )

                Button(
                    onClick = { handleButtonClick(ButtonType.ADD) },
                    modifier = defModifier,
                    enabled = imagePath.isNotBlank(),
                ) { Text("Add") }

                Button(
                    enabled = imagePath.isNotBlank(),
                    onClick = { handleButtonClick(ButtonType.NEGATE) },
                    modifier = defModifier,
                ) { Text("Negate") }

                Button(
                    enabled = imagePath.isNotBlank(),
                    onClick = { handleButtonClick(ButtonType.MULTIPLY) },
                    modifier = defModifier,
                ) { Text("Multiply") }

                Button(
                    enabled = imagePath.isNotBlank(),
                    onClick = { handleButtonClick(ButtonType.POWER) },
                    modifier = defModifier,
                ) { Text("Power") }

                Button(
                    enabled = imagePath.isNotBlank(),
                    onClick = { handleButtonClick(ButtonType.LOG) },
                    modifier = defModifier,
                ) { Text("Log") }

                Text("***", modifier = Modifier.padding(2.dp))

                Button(
                    enabled = imagePath.isNotBlank(),
                    onClick = { handleButtonClick(ButtonType.LINEAR_CONTR) },
                    modifier = defModifier,
                ) { Text("Linear contrasting") }

                Text("***", modifier = Modifier.padding(2.dp))

                Button(
                    enabled = imagePath.isNotBlank(),
                    onClick = { handleButtonClick(ButtonType.ADAPTIVE) },
                    modifier = defModifier,
                ) { Text("Adaptive method") }

                Text("Local methods", modifier = Modifier.padding(2.dp))

                OutlinedTextField(
                    value = localMethodRValue,
                    modifier = defModifier,
                    onValueChange = {
                        val replaced = it.text.replace(Regex("[^0-9]+"), "")
                        localMethodRValue = if (replaced == it.text) {
                            it.copy(replaced)
                        } else {
                            it.copy(replaced, TextRange(it.selection.end - 1))
                        }
                    },
                    label = { Text("Pixel neighbourhood size") },
                )

                Button(
                    enabled = imagePath.isNotBlank(),
                    onClick = { handleButtonClick(ButtonType.BERNSEN) },
                    modifier = defModifier,
                ) { Text("Bernsen") }

                Button(
                    enabled = imagePath.isNotBlank(),
                    onClick = { handleButtonClick(ButtonType.SAUVOLA) },
                    modifier = defModifier,
                ) { Text("Sauvola") }
            }
            VerticalScrollbar(adapter = rememberScrollbarAdapter(scrollState = state))

            Column(modifier = Modifier.fillMaxSize()) {
                if (imagePath.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            imageFromFile(File(imagePath)),
                            modifier = Modifier.fillMaxWidth(0.5f).padding(2.dp)
                        )
                        if (update) {
                            Image(
                                imageFromFile(File(modifiedImagePath)),
                                modifier = defModifier
                            )
                        }
                    }
                }
            }
        }
    }
}
