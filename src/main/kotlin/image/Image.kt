package ch.guengel.imageserver.image


import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import java.io.File
import java.io.OutputStream
import javax.imageio.ImageIO

class Image(bufferedImage: BufferedImage) {
    private val image: BufferedImage = bufferedImage
    val width get() = image.width
    val height get() = image.height

    constructor(imageFile: File) : this(ImageIO.read(imageFile))

    fun resizeToMatch(targetWidth: Int, targetHeight: Int): Image {
        val outputImage = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB)
        val affineTransform: AffineTransform

        if (targetWidth == targetHeight) {
            if (width > height) {
                val scaleFactor = targetHeight / height.toDouble()
                affineTransform = AffineTransform.getScaleInstance(scaleFactor, scaleFactor)
            } else {
                val scaleFactor = targetWidth / width.toDouble()
                affineTransform = AffineTransform.getScaleInstance(scaleFactor, scaleFactor)

            }
        } else if (targetWidth > targetHeight) {
            val scaleFactor = targetWidth / width.toDouble()
            affineTransform = AffineTransform.getScaleInstance(scaleFactor, scaleFactor)
        } else {
            val scaleFactor = targetHeight / height.toDouble()
            affineTransform = AffineTransform.getScaleInstance(scaleFactor, scaleFactor)
        }

        val scaleOp = AffineTransformOp(affineTransform, AffineTransformOp.TYPE_BILINEAR)
        scaleOp.filter(image, outputImage)

        return Image(outputImage)
    }

    fun writePNG(outputStream: OutputStream) {
        ImageIO.write(image, "png", outputStream)
    }

    companion object {
        val imagePattern = ".*\\.(?:jpe?g|png|gif)$"
    }

}