package canvas

import dev.arbjerg.lavalink.protocol.v4.TrackInfo
import dev.pierrot.canvas.MusicCardOption
import net.dv8tion.jda.api.utils.FileUpload
import org.apache.batik.transcoder.TranscoderException
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO

@Throws(IOException::class, TranscoderException::class)
fun generateImage(option: MusicCardOption): ByteArrayInputStream {
    if (option.progress == null) option.progress = 10
    if (option.name == null) option.name = "IamPierrot"
    if (option.author == null) option.author = "By Pierrot"

    if (option.progressBarColor == null) option.progressBarColor = "#5F2D00"
    if (option.progressColor == null) option.progressColor = "#FF7A00"
    if (option.backgroundColor == null) option.backgroundColor = "#070707"
    if (option.nameColor == null) option.nameColor = "#FF7A00"
    if (option.authorColor == null) option.authorColor = "#FFFFFF"
    if (option.imageDarkness == null) option.imageDarkness = 10

    val noImageSvg = generateSvg(
        """<svg width="837" height="837" viewBox="0 0 837 837" fill="none" xmlns="http://www.w3.org/2000/svg">
    <rect width="837" height="837" fill="${option.progressColor}"/>
    <path d="M419.324 635.912C406.035 635.912 394.658 631.18 385.195 621.717C375.732 612.254 371 600.878 371 587.589C371 574.3 375.732 562.923 385.195 553.46C394.658 543.997 406.035 539.265 419.324 539.265C432.613 539.265 443.989 543.997 453.452 553.46C462.915 562.923 467.647 574.3 467.647 587.589C467.647 600.878 462.915 612.254 453.452 621.717C443.989 631.18 432.613 635.912 419.324 635.912ZM371 490.941V201H467.647V490.941H371Z" fill="${option.backgroundColor}"/>
    </svg>"""
    )

    if (option.thumbnailImage == null) {
        option.thumbnailImage = noImageSvg
    }

    val thumbnail = try {
        cropImage(
            CropOption()
                .setImagePath(option.thumbnailImage!!)
                .setCircle(true)
                .setWidth(400)
                .setHeight(400)
                .setX(0)
                .setY(0)
                .setCropCenter(true)
        )
    } catch (e: Exception) {
        cropImage(
            CropOption()
                .setImagePath(noImageSvg)
                .setCircle(true)
                .setWidth(400)
                .setHeight(400)
                .setX(0)
                .setY(0)
                .setCropCenter(true)
        )
    }

    if (option.progress!! < 10) {
        option.progress = 10
    } else if (option.progress!! >= 100) {
        option.progress = 100
    }

    if (option.name!!.length > 20) {
        option.name = option.name!!.substring(0, 20) + "..."
    }

    if (option.author!!.length > 20) {
        option.author = option.author!!.substring(0, 20) + "..."
    }

    val canvas = BufferedImage(2367, 520, BufferedImage.TYPE_INT_ARGB)
    val ctx = canvas.createGraphics()

    if (option.backgroundImage != null) {
        try {
            val darknessSvg = generateSvg(
                """<svg width="2367" height="520" viewBox="0 0 2367 520" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M0 0H2367V520H0V0Z" fill="#070707" fill-opacity="${option.imageDarkness!! / 100.0}"/>
                </svg>"""
            )

            val image = cropImage(
                CropOption()
                    .setImagePath(option.backgroundImage!!)
                    .setX(0)
                    .setY(0)
                    .setWidth(2367)
                    .setHeight(520)
                    .setCropCenter(true)
            )
            ctx.drawImage(image, 0, 0, null)
            ctx.drawImage(loadImage(darknessSvg), 0, 0, null)
        } catch (e: Exception) {
            val backgroundSvg = generateSvg(
                """<svg width="2367" height="520" viewBox="0 0 2367 520" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M0 260C0 116.406 116.406 0 260 0H2107C2250.59 0 2367 116.406 2367 260V260C2367 403.594 2250.59 520 2107 520H260C116.406 520 0 403.594 0 260V260Z" fill="${option.backgroundColor}"/>
                </svg>"""
            )

            val background = loadImage(backgroundSvg)
            ctx.drawImage(background, 0, 0, null)
        }
    } else {
        val backgroundSvg = generateSvg(
            """<svg width="2367" height="520" viewBox="0 0 2367 520" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M0 260C0 116.406 116.406 0 260 0H2107C2250.59 0 2367 116.406 2367 260V260C2367 403.594 2250.59 520 2107 520H260C116.406 520 0 403.594 0 260V260Z" fill="${option.backgroundColor}"/>
    </svg>"""
        )

        val background = loadImage(backgroundSvg)
        ctx.drawImage(background, 0, 0, null)
    }

    ctx.drawImage(thumbnail, 69, 61, null)

    ctx.color = Color.decode(option.progressBarColor)
    ctx.stroke = BasicStroke(35f)
    ctx.drawOval(1945, 105, 310, 310)

    val angle = (option.progress!! / 100.0) * 2 * Math.PI

    ctx.color = Color.decode(option.progressColor)
    ctx.stroke = BasicStroke(35f)
    ctx.drawArc(1945, 105, 310, 310, -90, Math.toDegrees(angle).toInt())

    ctx.color = Color.decode(option.nameColor)
    ctx.font = Font("Extrabold", Font.BOLD, 100)
    option.name?.let { ctx.drawString(it, 550, 240) }

    ctx.color = Color.decode(option.authorColor)
    ctx.font = Font("Semibold", Font.BOLD, 70)
    option.author?.let { ctx.drawString(it, 550, 350) }

    ctx.dispose()

    val baos = ByteArrayOutputStream()
    ImageIO.write(canvas, "png", baos)
    return ByteArrayInputStream(baos.toByteArray())
}

@Throws(TranscoderException::class, IOException::class)
fun getMusicCard(trackInfo: TrackInfo): FileUpload {
    val colorPalette = arrayOf(
        "#070707",
        "#0D0D0D",
        "#1A1A1A",
        "#262626",
        "#333333",
        "#404040"
    )
    val artworkImage = trackInfo.artworkUrl

    val random = Random()
    val backGroundColor = colorPalette[random.nextInt(colorPalette.size)]
    val lightColors = getLightColors(artworkImage)
    val dominantColor = lightColors.first()
    val option = getMusicCardOption(dominantColor, artworkImage, backGroundColor, trackInfo)

    return FileUpload.fromData(generateImage(option), "card.png")
}

private fun getMusicCardOption(
    dominantColor: Color,
    artworkImage: String?,
    backGroundColor: String,
    trackInfo: TrackInfo
): MusicCardOption {
    val dominantColorHex = rgbToHex(dominantColor.red, dominantColor.green, dominantColor.blue)

    val option = MusicCardOption()
    option.backgroundImage = artworkImage
    option.imageDarkness = 80
    option.thumbnailImage = artworkImage
    option.backgroundColor = backGroundColor
    option.progress = 1
    option.progressColor = dominantColorHex
    option.progressBarColor = "#9C9C9C"
    option.name = trackInfo.title
    option.nameColor = dominantColorHex
    option.author = trackInfo.author
    option.authorColor = "#696969"
    option.startTime = "0:10"
    option.endTime = "3:45"
    option.timeColor = dominantColorHex
    return option
}

@Throws(TranscoderException::class, IOException::class)
private fun getLightColors(imageUrl: String?): List<Color> {
    requireNotNull(imageUrl) { "Image URL must not be null" }

    val image = loadImage(imageUrl)
    val lightColors = mutableListOf<Color>()
    val width = image.width
    val height = image.height
    outerLoop@ for (x in 0 until width) {
        for (y in 0 until height) {
            val rgb = image.getRGB(x, y)
            val color = Color(rgb, true)
            if (isLightColor(color)) {
                lightColors.add(color)
                break@outerLoop
            }
        }
    }

    return lightColors
}

private fun isLightColor(color: Color): Boolean {
    val rgbComponents = color.getRGBColorComponents(null)
    val brightness = (0.299 * rgbComponents[0] + 0.587 * rgbComponents[1] + 0.114 * rgbComponents[2])
    return brightness > 0.7
}

// Method to convert RGB to Hex
private fun rgbToHex(r: Int, g: Int, b: Int): String {
    return "#%02X%02X%02X".format(r, g, b)
}
