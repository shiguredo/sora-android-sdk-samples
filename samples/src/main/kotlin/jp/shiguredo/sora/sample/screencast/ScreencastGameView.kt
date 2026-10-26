package jp.shiguredo.sora.sample.screencast

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Choreographer
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import jp.shiguredo.sora.sample.R
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/**
 * スクリーンキャストで配信する箱積みゲームの View です。
 *
 * 画面をタップすると、タップした位置に箱が出現して落下します。
 * 箱は回転を含む 2D 剛体として扱い、床や他の箱との接触、摩擦、トルクを計算します。
 * 重心が支えから外れた箱は傾いて落ち、積み上げた箱に新しい箱がぶつかると崩れることがあります。
 * MediaProjection は画面が更新されたときにしかフレームを送信しないため、
 * 表示中は 30fps で描画し続けて配信映像が止まらないようにしています。
 * サンプルとして 1 ファイルで完結させるため、物理演算もこの View 内に実装しています。
 */
class ScreencastGameView : View {
    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private class Box(
        var x: Float,
        var y: Float,
        var angle: Float,
        var velocityX: Float,
        var velocityY: Float,
        var angularVelocity: Float,
        val size: Float,
        val color: Int,
    ) {
        val half: Float = size / 2f
        val inverseMass: Float = 1f
        val inverseInertia: Float = 6f / (size * size)

        var extent: Float = half
        var sleeping: Boolean = false
        var sleepFrames: Int = 0

        fun wake() {
            sleeping = false
            sleepFrames = 0
        }
    }

    private class ContactPoint(
        val x: Float,
        val y: Float,
    ) {
        var normalImpulse: Float = 0f
        var tangentImpulse: Float = 0f
    }

    private class Contact(
        val boxA: Box,
        val boxB: Box?,
        val normalX: Float,
        val normalY: Float,
        val penetration: Float,
        val points: List<ContactPoint>,
    )

    companion object {
        private const val MAX_BOX_COUNT = 100
        private const val SOLVER_ITERATIONS = 16
        private const val MAX_STEP_SECONDS = 1f / 180f
        private const val MAX_FRAME_SECONDS = 0.05f

        // 画面キャプチャの 30fps に合わせた描画間隔 (33ms)
        private const val DRAW_INTERVAL_NANOS = 33_000_000L

        private const val GRAVITY_DP_PER_SECOND2 = 2600f
        private const val BOX_SIZE_DP = 56f
        private const val FLOOR_HEIGHT_DP = 16f
        private const val HINT_TEXT_SIZE_SP = 14f
        private const val CONTACT_FRICTION = 0.8f
        private const val PENETRATION_SLOP_DP = 0.5f
        private const val POSITION_CORRECTION = 0.3f
        private const val LINEAR_DAMPING_PER_SECOND = 0.1f
        private const val ANGULAR_DAMPING_PER_SECOND = 1.5f
        private const val MAX_LINEAR_SPEED_DP_PER_SECOND = 6000f
        private const val MAX_ANGULAR_SPEED = 20f
        private const val SLEEP_LINEAR_SPEED_DP_PER_SECOND = 20f
        private const val SLEEP_ANGULAR_SPEED = 0.1f
        private const val SLEEP_FRAMES = 30
    }

    private val boxes = mutableListOf<Box>()
    private val contacts = mutableListOf<Contact>()
    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val floorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hintText = context.getString(R.string.screencast_game_hint)

    private val density = resources.displayMetrics.density
    private val boxSize = BOX_SIZE_DP * density
    private val floorHeight = FLOOR_HEIGHT_DP * density
    private val gravity = GRAVITY_DP_PER_SECOND2 * density
    private val maxLinearSpeed = MAX_LINEAR_SPEED_DP_PER_SECOND * density
    private val penetrationSlop = PENETRATION_SLOP_DP * density
    private val sleepLinearSpeed = SLEEP_LINEAR_SPEED_DP_PER_SECOND * density
    private val choreographer = Choreographer.getInstance()

    private var floorTop = 0f
    private var floorLeft = 0f
    private var floorRight = 0f

    private var isRendering = false
    private var lastFrameNanos = 0L
    private var lastDrawNanos = 0L

    private val frameCallback =
        object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (!isRendering) {
                    return
                }
                val deltaSeconds =
                    if (lastFrameNanos == 0L) {
                        0f
                    } else {
                        ((frameTimeNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0f, MAX_FRAME_SECONDS)
                    }
                lastFrameNanos = frameTimeNanos
                if (deltaSeconds > 0f) {
                    updatePhysics(deltaSeconds)
                }
                // キャプチャは 30fps のため、描画も 30fps に間引く
                if (frameTimeNanos - lastDrawNanos >= DRAW_INTERVAL_NANOS) {
                    lastDrawNanos = frameTimeNanos
                    invalidate()
                }
                choreographer.postFrameCallback(this)
            }
        }

    init {
        isClickable = true
        contentDescription = hintText
        floorPaint.color = Color.rgb(0x22, 0x22, 0x22)
        hintPaint.color = Color.argb(120, 0x33, 0x33, 0x33)
        hintPaint.textAlign = Paint.Align.CENTER
        hintPaint.textSize =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, HINT_TEXT_SIZE_SP, resources.displayMetrics)
    }

    /**
     * 描画と物理演算を開始します。
     */
    fun startRendering() {
        if (isRendering) {
            return
        }
        isRendering = true
        lastFrameNanos = 0L
        lastDrawNanos = 0L
        choreographer.postFrameCallback(frameCallback)
    }

    /**
     * 描画と物理演算を停止します。
     */
    fun stopRendering() {
        if (!isRendering) {
            return
        }
        isRendering = false
        choreographer.removeFrameCallback(frameCallback)
    }

    override fun onDetachedFromWindow() {
        stopRendering()
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(
        width: Int,
        height: Int,
        oldWidth: Int,
        oldHeight: Int,
    ) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        floorTop = max(0f, height - floorHeight)
        val margin = boxSize.coerceAtMost(width / 4f)
        floorLeft = margin
        floorRight = width - margin

        for (box in boxes) {
            val extent = boxExtent(box)
            box.x = box.x.coerceIn(extent, max(extent, width - extent))
            box.y = box.y.coerceAtMost(floorTop - box.half)
            box.wake()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> return true
            MotionEvent.ACTION_UP -> {
                spawnBox(event.x, event.y)
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                // override した performClick は TalkBack 用に画面中央へ箱を追加するため、
                // タップ位置への追加はここで行い、アクセシビリティイベントだけ super に任せる
                super.performClick()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        // TalkBack のダブルタップでも箱を落とせるように、画面中央に箱を追加する
        spawnBox(width / 2f, floorTop / 2f)
        return super.performClick()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.WHITE)
        canvas.drawRect(floorLeft, floorTop, floorRight, height.toFloat(), floorPaint)

        for (box in boxes) {
            boxPaint.color = box.color
            val half = box.half
            canvas.save()
            canvas.rotate(box.angle * 180f / PI.toFloat(), box.x, box.y)
            canvas.drawRect(box.x - half, box.y - half, box.x + half, box.y + half, boxPaint)
            canvas.restore()
        }

        if (boxes.isEmpty()) {
            canvas.drawText(
                hintText,
                width / 2f,
                floorTop / 2f,
                hintPaint,
            )
        }
    }

    private fun spawnBox(
        tapX: Float,
        tapY: Float,
    ) {
        if (width == 0 || height == 0) {
            return
        }
        val half = boxSize / 2f
        val x = tapX.coerceIn(half, max(half, width - half))
        var y = tapY.coerceIn(half, max(half, floorTop - half))
        // 既存の箱と重なる位置に出現すると箱が跳ねるため、重なりがなくなるまで上にずらす
        var remaining = boxes.size + 1
        while (remaining-- > 0) {
            val box =
                boxes.firstOrNull { candidate ->
                    val extent = boxExtent(candidate)
                    abs(x - candidate.x) < extent + half && abs(y - candidate.y) < extent + half
                } ?: break
            y = (box.y - boxExtent(box) - half).coerceAtLeast(half)
        }
        boxes.add(
            Box(
                x = x,
                y = y,
                angle = 0f,
                velocityX = 0f,
                velocityY = 0f,
                angularVelocity = 0f,
                size = boxSize,
                color = randomBoxColor(),
            ),
        )
        // 上限に達したら最古の箱（通常は最下段）を削除する
        if (boxes.size > MAX_BOX_COUNT) {
            boxes.removeAt(0)
        }
    }

    private fun randomBoxColor(): Int {
        val hsv =
            floatArrayOf(
                Random.nextFloat() * 360f,
                0.55f + Random.nextFloat() * 0.45f,
                0.75f + Random.nextFloat() * 0.25f,
            )
        return Color.HSVToColor(hsv)
    }

    private fun updatePhysics(deltaSeconds: Float) {
        val subStepCount = ceil(deltaSeconds / MAX_STEP_SECONDS).toInt()
        val subStepSeconds = deltaSeconds / subStepCount
        repeat(subStepCount) {
            integrate(subStepSeconds)
            buildContacts()
            repeat(SOLVER_ITERATIONS) { iteration ->
                // 接触の解決順を交互に反転させて、一方向への偏りを抑える
                solveVelocityConstraints(iteration % 2 == 1)
            }
            correctPositions()
        }
        removeOutOfBoundsBoxes()
        updateSleeping()
    }

    private fun integrate(stepSeconds: Float) {
        val linearDamping = (1f - LINEAR_DAMPING_PER_SECOND * stepSeconds).coerceAtLeast(0f)
        val angularDamping = (1f - ANGULAR_DAMPING_PER_SECOND * stepSeconds).coerceAtLeast(0f)
        for (box in boxes) {
            if (box.sleeping) {
                continue
            }
            box.velocityY += gravity * stepSeconds
            box.velocityX = (box.velocityX * linearDamping).coerceIn(-maxLinearSpeed, maxLinearSpeed)
            box.velocityY = (box.velocityY * linearDamping).coerceIn(-maxLinearSpeed, maxLinearSpeed)
            box.angularVelocity =
                (box.angularVelocity * angularDamping).coerceIn(-MAX_ANGULAR_SPEED, MAX_ANGULAR_SPEED)
            box.x += box.velocityX * stepSeconds
            box.y += box.velocityY * stepSeconds
            box.angle += box.angularVelocity * stepSeconds
        }
    }

    private fun buildContacts() {
        contacts.clear()
        for (box in boxes) {
            if (box.sleeping) {
                continue
            }
            box.extent = boxExtent(box)
            buildFloorContact(box)
        }
        for (i in boxes.indices) {
            for (j in i + 1 until boxes.size) {
                val boxA = boxes[i]
                val boxB = boxes[j]
                if (boxA.sleeping && boxB.sleeping) {
                    continue
                }
                val contact = createBoxContact(boxA, boxB) ?: continue
                if (boxA.sleeping) {
                    boxA.wake()
                }
                if (boxB.sleeping) {
                    boxB.wake()
                }
                contacts.add(contact)
            }
        }
    }

    private fun buildFloorContact(box: Box) {
        if (box.y + box.extent < floorTop) {
            return
        }
        val cosAngle = cos(box.angle.toDouble()).toFloat()
        val sinAngle = sin(box.angle.toDouble()).toFloat()
        val points = ArrayList<ContactPoint>(2)
        var maximumPenetration = 0f
        for (index in 0 until 4) {
            val localX = if (index == 0 || index == 3) -box.half else box.half
            val localY = if (index < 2) -box.half else box.half
            val cornerX = box.x + localX * cosAngle - localY * sinAngle
            val cornerY = box.y + localX * sinAngle + localY * cosAngle
            if (cornerX < floorLeft || cornerX > floorRight) {
                continue
            }
            val penetration = cornerY - floorTop
            if (penetration <= 0f) {
                continue
            }
            if (penetration > maximumPenetration) {
                maximumPenetration = penetration
            }
            addContactPoint(points, cornerX, cornerY)
        }
        if (points.isEmpty()) {
            return
        }
        val selected = selectContactPoints(points, 0f, 1f)
        contacts.add(Contact(box, null, 0f, 1f, maximumPenetration, selected))
    }

    private fun createBoxContact(
        boxA: Box,
        boxB: Box,
    ): Contact? {
        val extentA = boxA.extent
        val extentB = boxB.extent
        if (abs(boxB.x - boxA.x) > extentA + extentB ||
            abs(boxB.y - boxA.y) > extentA + extentB
        ) {
            return null
        }

        val cosA = cos(boxA.angle.toDouble()).toFloat()
        val sinA = sin(boxA.angle.toDouble()).toFloat()
        val cosB = cos(boxB.angle.toDouble()).toFloat()
        val sinB = sin(boxB.angle.toDouble()).toFloat()

        var minimumOverlap = Float.MAX_VALUE
        var normalX = 0f
        var normalY = 0f
        for (axisIndex in 0 until 4) {
            var axisX = 0f
            var axisY = 0f
            when (axisIndex) {
                0 -> {
                    axisX = cosA
                    axisY = sinA
                }
                1 -> {
                    axisX = -sinA
                    axisY = cosA
                }
                2 -> {
                    axisX = cosB
                    axisY = sinB
                }
                else -> {
                    axisX = -sinB
                    axisY = cosB
                }
            }
            val distance = (boxB.x - boxA.x) * axisX + (boxB.y - boxA.y) * axisY
            val radiusA =
                boxA.half *
                    (abs(cosA * axisX + sinA * axisY) + abs(-sinA * axisX + cosA * axisY))
            val radiusB =
                boxB.half *
                    (abs(cosB * axisX + sinB * axisY) + abs(-sinB * axisX + cosB * axisY))
            val overlap = radiusA + radiusB - abs(distance)
            if (overlap <= 0f) {
                return null
            }
            if (overlap < minimumOverlap) {
                minimumOverlap = overlap
                if (distance >= 0f) {
                    normalX = axisX
                    normalY = axisY
                } else {
                    normalX = -axisX
                    normalY = -axisY
                }
            }
        }

        val points = ArrayList<ContactPoint>(4)
        addCornersInside(boxB, boxA, points)
        addCornersInside(boxA, boxB, points)
        if (points.isEmpty()) {
            val radiusA =
                boxA.half *
                    (abs(cosA * normalX + sinA * normalY) + abs(-sinA * normalX + cosA * normalY))
            val radiusB =
                boxB.half *
                    (abs(cosB * normalX + sinB * normalY) + abs(-sinB * normalX + cosB * normalY))
            val contactX = (boxA.x + normalX * radiusA + boxB.x - normalX * radiusB) / 2f
            val contactY = (boxA.y + normalY * radiusA + boxB.y - normalY * radiusB) / 2f
            addContactPoint(points, contactX, contactY)
        }
        val selected = selectContactPoints(points, normalX, normalY)
        return Contact(boxA, boxB, normalX, normalY, minimumOverlap, selected)
    }

    private fun addCornersInside(
        source: Box,
        target: Box,
        points: MutableList<ContactPoint>,
    ) {
        val sourceCos = cos(source.angle.toDouble()).toFloat()
        val sourceSin = sin(source.angle.toDouble()).toFloat()
        val targetCos = cos(target.angle.toDouble()).toFloat()
        val targetSin = sin(target.angle.toDouble()).toFloat()
        for (index in 0 until 4) {
            val localX = if (index == 0 || index == 3) -source.half else source.half
            val localY = if (index < 2) -source.half else source.half
            val cornerX = source.x + localX * sourceCos - localY * sourceSin
            val cornerY = source.y + localX * sourceSin + localY * sourceCos
            val deltaX = cornerX - target.x
            val deltaY = cornerY - target.y
            val localTargetX = deltaX * targetCos + deltaY * targetSin
            val localTargetY = -deltaX * targetSin + deltaY * targetCos
            if (abs(localTargetX) <= target.half + penetrationSlop &&
                abs(localTargetY) <= target.half + penetrationSlop
            ) {
                addContactPoint(points, cornerX, cornerY)
            }
        }
    }

    private fun addContactPoint(
        points: MutableList<ContactPoint>,
        x: Float,
        y: Float,
    ) {
        val minimumDistanceSquared = boxSize * boxSize * 0.01f
        for (point in points) {
            val deltaX = point.x - x
            val deltaY = point.y - y
            if (deltaX * deltaX + deltaY * deltaY < minimumDistanceSquared) {
                return
            }
        }
        points.add(ContactPoint(x, y))
    }

    private fun selectContactPoints(
        points: MutableList<ContactPoint>,
        normalX: Float,
        normalY: Float,
    ): List<ContactPoint> {
        if (points.size <= 2) {
            return points
        }
        val tangentX = -normalY
        val tangentY = normalX
        var minimumPoint = points[0]
        var maximumPoint = points[0]
        var minimumProjection = Float.MAX_VALUE
        var maximumProjection = -Float.MAX_VALUE
        for (point in points) {
            val projection = point.x * tangentX + point.y * tangentY
            if (projection < minimumProjection) {
                minimumProjection = projection
                minimumPoint = point
            }
            if (projection > maximumProjection) {
                maximumProjection = projection
                maximumPoint = point
            }
        }
        return if (minimumPoint === maximumPoint) listOf(minimumPoint) else listOf(minimumPoint, maximumPoint)
    }

    private fun solveVelocityConstraints(reverse: Boolean) {
        val contactIndices = if (reverse) contacts.indices.reversed() else contacts.indices
        for (contactIndex in contactIndices) {
            val contact = contacts[contactIndex]
            val boxA = contact.boxA
            val boxB = contact.boxB
            val normalX = contact.normalX
            val normalY = contact.normalY
            val tangentX = -normalY
            val tangentY = normalX
            val inverseMassB = boxB?.inverseMass ?: 0f
            val inverseInertiaB = boxB?.inverseInertia ?: 0f

            val points = if (reverse) contact.points.asReversed() else contact.points
            for (point in points) {
                val radiusAX = point.x - boxA.x
                val radiusAY = point.y - boxA.y
                val radiusBX = if (boxB != null) point.x - boxB.x else 0f
                val radiusBY = if (boxB != null) point.y - boxB.y else 0f

                val velocityAX = boxA.velocityX - boxA.angularVelocity * radiusAY
                val velocityAY = boxA.velocityY + boxA.angularVelocity * radiusAX
                val velocityBX = if (boxB != null) boxB.velocityX - boxB.angularVelocity * radiusBY else 0f
                val velocityBY = if (boxB != null) boxB.velocityY + boxB.angularVelocity * radiusBX else 0f
                val relativeVelocityX = velocityBX - velocityAX
                val relativeVelocityY = velocityBY - velocityAY
                val normalVelocity = relativeVelocityX * normalX + relativeVelocityY * normalY

                val radiusNormalA = radiusAX * normalY - radiusAY * normalX
                val radiusNormalB = radiusBX * normalY - radiusBY * normalX
                val inverseMassSum =
                    boxA.inverseMass + inverseMassB +
                        boxA.inverseInertia * radiusNormalA * radiusNormalA +
                        inverseInertiaB * radiusNormalB * radiusNormalB
                val targetNormalVelocity = 0f
                val normalImpulse = (targetNormalVelocity - normalVelocity) / inverseMassSum
                val oldNormalImpulse = point.normalImpulse
                val newNormalImpulse = max(oldNormalImpulse + normalImpulse, 0f)
                val appliedNormalImpulse = newNormalImpulse - oldNormalImpulse
                point.normalImpulse = newNormalImpulse
                if (appliedNormalImpulse != 0f) {
                    boxA.velocityX -= appliedNormalImpulse * normalX * boxA.inverseMass
                    boxA.velocityY -= appliedNormalImpulse * normalY * boxA.inverseMass
                    boxA.angularVelocity -= appliedNormalImpulse * radiusNormalA * boxA.inverseInertia
                    if (boxB != null) {
                        boxB.velocityX += appliedNormalImpulse * normalX * boxB.inverseMass
                        boxB.velocityY += appliedNormalImpulse * normalY * boxB.inverseMass
                        boxB.angularVelocity += appliedNormalImpulse * radiusNormalB * boxB.inverseInertia
                    }
                }

                val frictionVelocityAX = boxA.velocityX - boxA.angularVelocity * radiusAY
                val frictionVelocityAY = boxA.velocityY + boxA.angularVelocity * radiusAX
                val frictionVelocityBX =
                    if (boxB != null) boxB.velocityX - boxB.angularVelocity * radiusBY else 0f
                val frictionVelocityBY =
                    if (boxB != null) boxB.velocityY + boxB.angularVelocity * radiusBX else 0f
                val frictionRelativeVelocityX = frictionVelocityBX - frictionVelocityAX
                val frictionRelativeVelocityY = frictionVelocityBY - frictionVelocityAY
                val tangentVelocity = frictionRelativeVelocityX * tangentX + frictionRelativeVelocityY * tangentY

                val radiusTangentA = radiusAX * tangentY - radiusAY * tangentX
                val radiusTangentB = radiusBX * tangentY - radiusBY * tangentX
                val tangentInverseMassSum =
                    boxA.inverseMass + inverseMassB +
                        boxA.inverseInertia * radiusTangentA * radiusTangentA +
                        inverseInertiaB * radiusTangentB * radiusTangentB
                val maximumFriction = CONTACT_FRICTION * point.normalImpulse
                val oldTangentImpulse = point.tangentImpulse
                val newTangentImpulse =
                    (oldTangentImpulse - tangentVelocity / tangentInverseMassSum)
                        .coerceIn(-maximumFriction, maximumFriction)
                val appliedTangentImpulse = newTangentImpulse - oldTangentImpulse
                point.tangentImpulse = newTangentImpulse
                if (appliedTangentImpulse != 0f) {
                    boxA.velocityX -= appliedTangentImpulse * tangentX * boxA.inverseMass
                    boxA.velocityY -= appliedTangentImpulse * tangentY * boxA.inverseMass
                    boxA.angularVelocity -= appliedTangentImpulse * radiusTangentA * boxA.inverseInertia
                    if (boxB != null) {
                        boxB.velocityX += appliedTangentImpulse * tangentX * boxB.inverseMass
                        boxB.velocityY += appliedTangentImpulse * tangentY * boxB.inverseMass
                        boxB.angularVelocity += appliedTangentImpulse * radiusTangentB * boxB.inverseInertia
                    }
                }
            }
        }
    }

    private fun correctPositions() {
        for (contact in contacts) {
            val boxA = contact.boxA
            val boxB = contact.boxB
            val inverseMassB = boxB?.inverseMass ?: 0f
            val inverseMassSum = boxA.inverseMass + inverseMassB
            if (inverseMassSum <= 0f) {
                continue
            }
            val correction =
                (contact.penetration - penetrationSlop).coerceAtLeast(0f) *
                    POSITION_CORRECTION / inverseMassSum
            if (correction <= 0f) {
                continue
            }
            boxA.x -= contact.normalX * correction * boxA.inverseMass
            boxA.y -= contact.normalY * correction * boxA.inverseMass
            if (boxB != null) {
                boxB.x += contact.normalX * correction * boxB.inverseMass
                boxB.y += contact.normalY * correction * boxB.inverseMass
            }
        }
    }

    private fun updateSleeping() {
        for (box in boxes) {
            if (box.sleeping) {
                continue
            }
            val speedSquared = box.velocityX * box.velocityX + box.velocityY * box.velocityY
            if (speedSquared <= sleepLinearSpeed * sleepLinearSpeed &&
                abs(box.angularVelocity) <= SLEEP_ANGULAR_SPEED
            ) {
                box.sleepFrames++
                if (box.sleepFrames >= SLEEP_FRAMES) {
                    box.sleeping = true
                    box.velocityX = 0f
                    box.velocityY = 0f
                    box.angularVelocity = 0f
                }
            } else {
                box.sleepFrames = 0
            }
        }
    }

    private fun removeOutOfBoundsBoxes() {
        boxes.removeAll { box ->
            val extent = boxExtent(box)
            box.y - extent > height + boxSize ||
                box.x + extent < -boxSize ||
                box.x - extent > width + boxSize
        }
    }

    private fun boxExtent(box: Box): Float {
        val angle = box.angle.toDouble()
        return box.half * (abs(cos(angle)).toFloat() + abs(sin(angle)).toFloat())
    }
}
