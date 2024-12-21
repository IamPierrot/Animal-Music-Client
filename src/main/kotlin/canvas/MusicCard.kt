package canvas

import dev.arbjerg.lavalink.protocol.v4.TrackInfo
import dev.pierrot.canvas.MusicCardOption
import net.dv8tion.jda.api.utils.FileUpload
import org.apache.batik.transcoder.TranscoderException
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO

@Throws(IOException::class, TranscoderException::class)
fun generateImage(option: MusicCardOption): ByteArrayInputStream {
    val canvas = BufferedImage(2367, 520, BufferedImage.TYPE_INT_ARGB)
    val ctx = canvas.createGraphics()

    try {
        with(option) {
            progress = progress?.coerceIn(10, 100) ?: 10
            name = name?.takeIf { it.length <= 20 } ?: name?.substring(0, 20)?.plus("...") ?: "IamPierrot"
            author = author?.takeIf { it.length <= 20 } ?: author?.substring(0, 20)?.plus("...") ?: "By Pierrot"

            val progressBarColor = progressBarColor ?: "#5F2D00"
            val progressColor = progressColor ?: "#FF7A00"
            val backgroundColor = backgroundColor ?: "#070707"
            val nameColor = nameColor ?: "#FF7A00"
            val authorColor = authorColor ?: "#FFFFFF"
            val imageDarkness = imageDarkness ?: 10

            val noImageSvg = generateSvg(
                """<svg width=\"837\" height=\"837\" viewBox=\"0 0 837 837\" fill=\"none\" xmlns=\"http://www.w3.org/2000/svg\">\n" +
                        "<rect width=\"837\" height=\"837\" fill=\"$progressColor\"/>\n" +
                        "<path d=\"M419.324 635.912C406.035 635.912 394.658 631.18 385.195 621.717C375.732 612.254 371 600.878 371 587.589C371 574.3 375.732 562.923 385.195 553.46C394.658 543.997 406.035 539.265 419.324 539.265C432.613 539.265 443.989 543.997 453.452 553.46C462.915 562.923 467.647 574.3 467.647 587.589C467.647 600.878 462.915 612.254 453.452 621.717C443.989 631.18 432.613 635.912 419.324 635.912ZM371 490.941V201H467.647V490.941H371Z\" fill=\"$backgroundColor\"/>\n" +
                        "</svg>"""
            )

            thumbnailImage = thumbnailImage ?: noImageSvg

            val thumbnail = try {
                cropImage(
                    CropOption()
                        .setImagePath(thumbnailImage!!)
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

            if (backgroundImage != null) {
                try {
                    val darknessSvg = generateSvg(
                        """<svg width=\"2367\" height=\"520\" viewBox=\"0 0 2367 520\" fill=\"none\" xmlns=\"http://www.w3.org/2000/svg\">\n" +
                                "<path d=\"M0 0H2367V520H0V0Z\" fill=\"#070707\" fill-opacity=\"${imageDarkness / 100.0}\"/>\n" +
                                "</svg>"""
                    )

                    val image = cropImage(
                        CropOption()
                            .setImagePath(backgroundImage!!)
                            .setX(0)
                            .setY(0)
                            .setWidth(2367)
                            .setHeight(520)
                            .setCropCenter(true)
                    )
                    ctx.drawImage(image, 0, 0, null)
                    ctx.drawImage(loadImage(darknessSvg), 0, 0, null)
                } catch (e: Exception) {
                    drawBackground(ctx, backgroundColor)
                }
            } else {
                drawBackground(ctx, backgroundColor)
            }

            ctx.drawImage(thumbnail, 69, 61, null)

            drawProgress(ctx, progress!!, progressBarColor, progressColor)

            ctx.color = Color.decode(nameColor)
            ctx.font = Font("Extrabold", Font.BOLD, 100)
            ctx.drawString(name!!, 550, 240)

            ctx.color = Color.decode(authorColor)
            ctx.font = Font("Semibold", Font.BOLD, 70)
            ctx.drawString(author!!, 550, 350)
        }

        val baos = ByteArrayOutputStream()
        ImageIO.write(canvas, "png", baos)
        return ByteArrayInputStream(baos.toByteArray())
    } finally {
        ctx.dispose()
    }
}

private fun drawBackground(ctx: Graphics2D, colorHex: String) {
    val backgroundSvg = generateSvg(
        """<svg width=\"2367\" height=\"520\" viewBox=\"0 0 2367 520\" fill=\"none\" xmlns=\"http://www.w3.org/2000/svg\">\n" +
                "<path d=\"M0 260C0 116.406 116.406 0 260 0H2107C2250.59 0 2367 116.406 2367 260V260C2367 403.594 2250.59 520 2107 520H260C116.406 520 0 403.594 0 260V260Z\" fill=\"$colorHex\"/>\n" +
                "</svg>"""
    )

    val background = loadImage(backgroundSvg)
    ctx.drawImage(background, 0, 0, null)
}

private fun drawProgress(ctx: Graphics2D, progress: Int, barColorHex: String, progressColorHex: String) {
    ctx.color = Color.decode(barColorHex)
    ctx.stroke = BasicStroke(35f)
    ctx.drawOval(1945, 105, 310, 310)

    val angle = (progress / 100.0) * 2 * Math.PI

    ctx.color = Color.decode(progressColorHex)
    ctx.stroke = BasicStroke(35f)
    ctx.drawArc(1945, 105, 310, 310, -90, Math.toDegrees(angle).toInt())
}

@Throws(TranscoderException::class, IOException::class)
fun getMusicCard(trackInfo: TrackInfo): FileUpload {
    // Bảng màu nền mặc định
    val colorPalette = arrayOf(
        "#070707",
        "#0D0D0D",
        "#1A1A1A",
        "#262626",
        "#333333",
        "#404040"
    )

    // URL ảnh artwork
    val artworkImage = trackInfo.artworkUrl

    // Chọn màu nền ngẫu nhiên từ bảng màu
    val random = Random()
    val backgroundColor = colorPalette[random.nextInt(colorPalette.size)]

    // Lấy màu sắc chủ đạo từ ảnh artwork
    val dominantColor = getLightColors(artworkImage).first()

    // Tạo cấu hình MusicCardOption
    val option = getMusicCardOption(dominantColor, artworkImage, backgroundColor, trackInfo)

    // Tạo hình ảnh và trả về dưới dạng FileUpload
    return FileUpload.fromData(generateImage(option), "card.png")
}

private fun getMusicCardOption(
    dominantColor: Color,
    artworkImage: String?,
    backgroundColor: String,
    trackInfo: TrackInfo
): MusicCardOption {
    // Chuyển đổi màu chủ đạo sang dạng mã hex
    val dominantColorHex = rgbToHex(dominantColor.red, dominantColor.green, dominantColor.blue)

    // Tạo MusicCardOption và gán giá trị
    return MusicCardOption().apply {
        this.backgroundImage = artworkImage
        this.imageDarkness = 80
        this.thumbnailImage = artworkImage
        this.backgroundColor = backgroundColor
        this.progress = 1
        this.progressColor = dominantColorHex
        this.progressBarColor = "#9C9C9C"
        this.name = trackInfo.title
        this.nameColor = dominantColorHex
        this.author = trackInfo.author
        this.authorColor = "#696969"
        this.startTime = "0:10"
        this.endTime = "3:45"
        this.timeColor = dominantColorHex
    }
}

@Throws(TranscoderException::class, IOException::class)
private fun getLightColors(imageUrl: String?): List<Color> {
    requireNotNull(imageUrl) { "Image URL must not be null" }

    val image = loadImage(imageUrl)
    val lightColors = mutableListOf<Color>()
    val width = image.width
    val height = image.height

    // Lấy các màu sáng từ ảnh
    for (x in 0 until width) {
        for (y in 0 until height) {
            val rgb = image.getRGB(x, y)
            val color = Color(rgb, true)
            if (isLightColor(color)) {
                lightColors.add(color)
                break
            }
        }
    }

    return lightColors
}

private fun isLightColor(color: Color): Boolean {
    // Kiểm tra độ sáng của màu dựa trên công thức
    val rgbComponents = color.getRGBColorComponents(null)
    val brightness = (0.299 * rgbComponents[0] + 0.587 * rgbComponents[1] + 0.114 * rgbComponents[2])
    return brightness > 0.7
}

private fun rgbToHex(r: Int, g: Int, b: Int): String {
    // Chuyển đổi RGB sang mã Hex
    return "#%02X%02X%02X".format(r, g, b)
}

