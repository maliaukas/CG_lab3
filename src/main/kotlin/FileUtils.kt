import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import org.jetbrains.skija.Image
import org.opencv.core.Mat
import org.opencv.imgcodecs.Imgcodecs
import java.io.File
import javax.swing.JFileChooser
import javax.swing.JPanel
import javax.swing.filechooser.FileNameExtensionFilter

fun loadImage(imagePath: String?): Mat? {
    return Imgcodecs.imread(imagePath)
}

fun imageFromFile(file: File): ImageBitmap {
    return Image.makeFromEncoded(file.readBytes()).asImageBitmap()
}

fun saveImage(imageMatrix: Mat?, targetPath: String?) {
    Imgcodecs.imwrite(targetPath, imageMatrix)
}

fun chooseFile(): String? {
    val chooser = JFileChooser()
    chooser.currentDirectory = File(".")
    chooser.dialogTitle = "Choose picture"
    chooser.fileSelectionMode = JFileChooser.FILES_ONLY
    val filter = FileNameExtensionFilter(
        "Images",
        "jpg", "JPG", "jpeg", "JPEG",
        "tif", "TIF", "tiff", "TIFF",
        "bmp", "BMP", "png", "PNG", "pcx", "PCX"
    )
    chooser.isAcceptAllFileFilterUsed = true
    chooser.fileFilter = filter

    return if (chooser.showOpenDialog(JPanel()) == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile.absolutePath
    } else {
        null
    }
}

fun tempFile(path: String): String {
    return path + "temp." + path.takeLastWhile { c -> c != '.' }
}