package ch.guengel.imageserver.image


import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import java.io.OutputStream
import java.nio.file.Path
import javax.imageio.ImageIO


class Image(bufferedImage: BufferedImage) {
    private val image: BufferedImage = removeAlpha(bufferedImage)
    val width get() = image.width
    val height get() = image.height

    constructor(imageFile: Path) : this(ImageIO.read(imageFile.toFile()))

    fun resizeToMatch(targetWidth: Int, targetHeight: Int): Image {
        val resizedImage = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB)
        val affineTransform: AffineTransform

        if (targetWidth == targetHeight) {
            affineTransform = if (width > height) {
                val scaleFactor = targetHeight / height.toDouble()
                AffineTransform.getScaleInstance(scaleFactor, scaleFactor)
            } else {
                val scaleFactor = targetWidth / width.toDouble()
                AffineTransform.getScaleInstance(scaleFactor, scaleFactor)

            }
        } else if (targetWidth > targetHeight) {
            val scaleFactor = targetWidth / width.toDouble()
            affineTransform = AffineTransform.getScaleInstance(scaleFactor, scaleFactor)
        } else {
            val scaleFactor = targetHeight / height.toDouble()
            affineTransform = AffineTransform.getScaleInstance(scaleFactor, scaleFactor)
        }

        val scaleOp = AffineTransformOp(affineTransform, AffineTransformOp.TYPE_BILINEAR)
        scaleOp.filter(image, resizedImage)

        return Image(removeAlpha(resizedImage))
    }

    private fun removeAlpha(resizedImage: BufferedImage): BufferedImage {
        val w = resizedImage.width
        val h = resizedImage.height
        val target = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)

        val g: Graphics2D = target.createGraphics()
        g.color = Color(0x0, false)
        g.fillRect(0, 0, w, h)
        g.drawImage(resizedImage, 0, 0, null)
        g.dispose()
        return target
    }

    fun write(outputStream: OutputStream) {
        ImageIO.write(image, "jpeg", outputStream)
    }

    companion object {
        private const val imagePattern = ".*\\.(?:jpe?g|png|gif)$"
        val imagePatternMatcher = Regex(imagePattern, RegexOption.IGNORE_CASE)

    }

}