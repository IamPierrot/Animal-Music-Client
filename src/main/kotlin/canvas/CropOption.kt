package canvas

class CropOption {
    var imagePath: String = ""
    var width: Int? = null
    var height: Int? = null
    var isCropCenter: Boolean = false
    var x: Int = 0
    var y: Int = 0
    var isCircle: Boolean = false
    var borderRadius: Int = 0

    fun setImagePath(imagePath: String): CropOption {
        this.imagePath = imagePath
        return this
    }

    fun setWidth(width: Int): CropOption {
        this.width = width
        return this
    }

    fun setHeight(height: Int?): CropOption {
        this.height = height
        return this
    }

    fun setCropCenter(cropCenter: Boolean): CropOption {
        this.isCropCenter = cropCenter
        return this
    }

    fun setX(x: Int): CropOption {
        this.x = x
        return this
    }

    fun setY(y: Int): CropOption {
        this.y = y
        return this
    }

    fun setCircle(circle: Boolean): CropOption {
        this.isCircle = circle
        return this
    }

    fun setBorderRadius(borderRadius: Int): CropOption {
        this.borderRadius = borderRadius
        return this
    }
}
