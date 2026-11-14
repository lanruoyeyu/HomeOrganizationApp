package com.fridgekeeper.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fridgekeeper.model.MapMarkerUiState
import com.fridgekeeper.model.MapPlaceUiState
import com.fridgekeeper.ui.theme.Primary

private val PlaceColor = Color(0xFFF57C00)

@Composable
fun RoomMap(
    markers: List<MapMarkerUiState>,
    places: List<MapPlaceUiState>,
    onTapMarker: (MapMarkerUiState) -> Unit,
    onTapPlace: (MapPlaceUiState) -> Unit,
    onTapBlank: (Float, Float) -> Unit,
    onLongPress: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(280.dp)
            .clip(RoundedCornerShape(16.dp))
            .pointerInput(markers, places) {
                detectTapGestures(
                    onTap = { offset ->
                        val size = this.size
                        places.forEach { place ->
                            val pos = Offset(
                                x = place.posX * size.width,
                                y = place.posY * size.height
                            )
                            if ((offset - pos).getDistance() <= 46f) {
                                onTapPlace(place)
                                return@detectTapGestures
                            }
                        }
                        markers.forEach { marker ->
                            val pos = Offset(
                                x = marker.posX * size.width,
                                y = marker.posY * size.height
                            )
                            if ((offset - pos).getDistance() <= 40f) {
                                onTapMarker(marker)
                                return@detectTapGestures
                            }
                        }
                        onTapBlank(offset.x / size.width, offset.y / size.height)
                    },
                    onLongPress = { offset ->
                        val size = this.size
                        val normX = offset.x / size.width
                        val normY = offset.y / size.height
                        if (normX in 0f..1f && normY in 0f..1f) {
                            onLongPress(normX, normY)
                        }
                    }
                )
            }
    ) {
        Canvas(
            modifier = Modifier.fillMaxWidth().height(280.dp)
        ) {
            val w = size.width
            val h = size.height

            // 房间背景
            drawRect(color = Color(0xFFF0F4F8))

            // 房间边框
            drawRect(
                color = Color(0xFFB0BEC5),
                size = size,
                style = Stroke(width = 4f)
            )

            // 分隔线（模拟房间区域）
            drawLine(
                color = Color(0xFFCFD8DC),
                start = Offset(w * 0.5f, 0f),
                end = Offset(w * 0.5f, h * 0.6f),
                strokeWidth = 3f
            )
            drawLine(
                color = Color(0xFFCFD8DC),
                start = Offset(0f, h * 0.6f),
                end = Offset(w, h * 0.6f),
                strokeWidth = 3f
            )

            // 具体位置点
            places.forEach { place ->
                val pos = Offset(place.posX * w, place.posY * h)
                drawCircle(
                    color = PlaceColor.copy(alpha = 0.25f),
                    radius = 22f,
                    center = pos
                )
                drawCircle(
                    color = PlaceColor,
                    radius = 12f,
                    center = pos
                )
                drawCircle(
                    color = Color.White,
                    radius = 4f,
                    center = pos
                )
            }

            // 物品标注点
            markers.forEach { marker ->
                val pos = Offset(marker.posX * w, marker.posY * h)
                // 外圈
                drawCircle(
                    color = Primary.copy(alpha = 0.3f),
                    radius = 22f,
                    center = pos
                )
                // 内圈
                drawCircle(
                    color = Primary,
                    radius = 14f,
                    center = pos
                )
                // 白色中心点
                drawCircle(
                    color = Color.White,
                    radius = 6f,
                    center = pos
                )
            }

            // 位置名称
            if (places.isNotEmpty()) {
                val textPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#37474F")
                    textSize = 12.sp.toPx()
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                places.forEach { place ->
                    drawContext.canvas.nativeCanvas.drawText(
                        place.name,
                        place.posX * w,
                        place.posY * h + 34f,
                        textPaint
                    )
                }
            }
        }

        // 空状态提示
        if (markers.isEmpty() && places.isEmpty()) {
            Text(
                text = "点击右上角 + 添加位置\n长按地图可标注物品",
                fontSize = 14.sp,
                color = Color(0xFF888888),
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
