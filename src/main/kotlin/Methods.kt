import org.opencv.core.Core
import org.opencv.core.CvType.CV_32F
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.Rect
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY
import org.opencv.imgproc.Imgproc.cvtColor
import kotlin.math.log10

// alpha * f(i,j) + beta
private fun linearFun(m: Mat, alpha: Double, beta: Double): Mat {
    val result = Mat.zeros(m.rows(), m.cols(), m.type())
    cvtColor(m, result, COLOR_BGR2GRAY)
    result.convertTo(result, -1, alpha, beta)
    return result
}

fun add(m: Mat, value: Double): Mat {
    return linearFun(m, 1.0, value)
}

fun negate(m: Mat): Mat {
    return linearFun(m, -1.0, 255.0)
}

fun mul(m: Mat, value: Double): Mat {
    return linearFun(m, value, 0.0)
}

fun power(m: Mat, value: Double): Mat {
    val r = Mat.zeros(m.rows(), m.cols(), m.type())
    cvtColor(m, r, COLOR_BGR2GRAY)
    r.convertTo(r, CV_32F)
    Core.pow(r, value, r)
    return r
}

private fun max(m: Mat): Double {
    val r = Core.minMaxLoc(m)
    return r.maxVal
}

private fun min(m: Mat): Double {
    val r = Core.minMaxLoc(m)
    return r.minVal
}

fun log(m: Mat): Mat {
    val r = Mat.zeros(m.rows(), m.cols(), m.type())
    cvtColor(m, r, COLOR_BGR2GRAY)
    val fMax = max(r)
    val const = 255.0 / log10(1 + fMax)
    for (i in 0 until r.rows()) {
        for (j in 0 until r.cols()) {
            r.put(i, j, const * log10(1 + r[i, j][0]))
        }
    }
    return r
}

fun linearContrast(m: Mat): Mat {
    val r = Mat.zeros(m.rows(), m.cols(), m.type())
    cvtColor(m, r, COLOR_BGR2GRAY)
    val fMax = max(r)
    val fMin = min(r)
    val const = 255.0 / (fMax - fMin)
    for (i in 0 until r.rows()) {
        for (j in 0 until r.cols()) {
            r.put(i, j, const * (r[i, j][0] - fMin))
        }
    }
    return r
}

fun adaptive(m: Mat): Mat {
    val r = Mat.zeros(m.rows(), m.cols(), m.type())
    cvtColor(m, r, COLOR_BGR2GRAY)
    Imgproc.adaptiveThreshold(
        r, r, 255.0,
        Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
        Imgproc.THRESH_BINARY,
        11,
        3.0
    )
    return r
}

enum class LocalMethodType {
    BERNSEN,
    SAUVOLA
}

fun localMethod(type: LocalMethodType, m: Mat, r: Int = 15, k: Double = 0.5, eps: Double = 15.0): Mat {
    val temp = Mat.zeros(m.rows(), m.cols(), m.type())
    cvtColor(m, temp, COLOR_BGR2GRAY)

    // r = 2 * border + 1
    val border = (r - 1) / 2
    val result = Mat(m.rows() + border * 2, m.cols() + border * 2, temp.type())
    Core.copyMakeBorder(temp, result, border, border, border, border, Core.BORDER_REPLICATE)
    val final = Mat.zeros(m.rows(), m.cols(), temp.type())

    for (i in border until result.rows() - border) {
        for (j in border until result.cols() - border) {
            val roi = result.submat(
                Rect(j - border, i - border, r, r)
            )

            val t =
                when (type) {
                    LocalMethodType.BERNSEN -> {
                        val minMax = Core.minMaxLoc(roi)

                        val c = (minMax.maxVal - minMax.minVal)
                        if (c > eps) (minMax.minVal + minMax.maxVal) / 2.0 else 0.0
                    }
                    LocalMethodType.SAUVOLA -> {
                        val mu = MatOfDouble()
                        val stddev = MatOfDouble()
                        Core.meanStdDev(roi, mu, stddev)

                        mu[0, 0][0] * (1 + k * (stddev[0, 0][0] / 128.0 - 1.0))
                    }
                }

            final.put(i - border, j - border, if (result[i, j][0] > t) 255.0 else 0.0)
        }
    }
    return final
}