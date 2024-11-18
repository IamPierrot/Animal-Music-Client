package canvas

import org.apache.batik.transcoder.TranscoderException
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.PNGTranscoder
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.*
import java.net.URL
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.max

@Throws(IOException::class, TranscoderException::class)
fun loadImage(imagePath: String?): BufferedImage {
    if (imagePath!!.startsWith("http://") || imagePath.startsWith("https://")) {
        return loadImageFromUrl(imagePath)
    } else if (imagePath.startsWith("data:image")) {
        val base64Image = imagePath.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
        val imageBytes = Base64.getDecoder().decode(base64Image)
        val bis = ByteArrayInputStream(imageBytes)

        return if (imagePath.startsWith("data:image/svg+xml")) {
            loadSvgImage(bis)
        } else {
            ImageIO.read(bis)
        }
    } else if (imagePath.endsWith(".svg")) {
        return loadSvgImage(FileInputStream(imagePath))
    } else {
        return ImageIO.read(File(imagePath))
    }
}

@Throws(IOException::class)
private fun loadImageFromUrl(imageUrl: String?): BufferedImage {
    val url = URL(imageUrl)
    return ImageIO.read(url)
}

@Throws(TranscoderException::class, IOException::class)
private fun loadSvgImage(inputStream: InputStream): BufferedImage {
    val transcoder = PNGTranscoder()
    val input = TranscoderInput(inputStream)
    val outputStream = ByteArrayOutputStream()
    val output = TranscoderOutput(outputStream)
    transcoder.transcode(input, output)
    outputStream.flush()

    val imageBytes = outputStream.toByteArray()
    val bis = ByteArrayInputStream(imageBytes)
    return ImageIO.read(bis)
}

@Throws(IOException::class, TranscoderException::class)
fun cropImage(option: CropOption?): BufferedImage {
    val image = loadImage(option?.imagePath)
    val width = option?.width ?: image.width
    val height =  option?.height ?: image.height

    val scaleWidth = width.toDouble() / image.width
    val scaleHeight = height.toDouble() / image.height
    val scaleFactor = max(scaleWidth, scaleHeight)

    val scaledWidth = (image.width * scaleFactor).toInt()
    val scaledHeight = (image.height * scaleFactor).toInt()

    var x = 0
    var y = 0
    if (option!!.isCropCenter) {
        x = (width - scaledWidth) / 2
        y = (height - scaledHeight) / 2
    } else {
        x = (option.x - (width - image.width * scaleFactor) / 2).toInt()
        y = (option.y - (height - image.height * scaleFactor) / 2).toInt()
    }

    val canvas = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val g2d = canvas.createGraphics()

    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY)

    if (option.isCircle) {
        g2d.clip = Ellipse2D.Float(0f, 0f, width.toFloat(), height.toFloat())
    } else if (option.borderRadius > 0) {
        g2d.clip = RoundRectangle2D.Double(
            0.0,
            0.0,
            width.toDouble(),
            height.toDouble(),
            option.borderRadius.toDouble(),
            option.borderRadius.toDouble()
        )
    }

    g2d.drawImage(image, x, y, scaledWidth, scaledHeight, null)
    g2d.dispose()

    return canvas
}

private fun correctSvgNamespace(svgContent: String): String {
    return svgContent.replace("http://www.w3.org.2000/svg", "http://www.w3.org/2000/svg")
}

fun generateSvg(svgContent: String): String {
    val svgContentConverted = correctSvgNamespace(svgContent)
    return "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svgContentConverted.toByteArray())
}
