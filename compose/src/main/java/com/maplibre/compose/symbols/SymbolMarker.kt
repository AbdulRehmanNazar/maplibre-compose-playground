/*
 * This file is part of ramani-maps.
 *
 * Copyright (c) 2023 Roman Bapst & Jonas Vautherin.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.maplibre.compose.symbols

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maplibre.compose.ramani.MapApplier
import com.maplibre.compose.ramani.MapLibreComposable
import com.maplibre.compose.ramani.SymbolNode
import com.maplibre.compose.symbols.builder.SymbolText
import com.maplibre.compose.symbols.models.SymbolOffset
import com.maplibre.compose.symbols.models.toMaplibreOffset
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.plugins.annotation.SymbolOptions
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.Property.ICON_ANCHOR_CENTER

@Composable
@MapLibreComposable
fun SymbolMarker(
    center: LatLng,
    label: String,
    mapImageId: String = "custom_marker_${label}",
    onTap: () -> Unit = {},
    onLongPress: () -> Unit = {}
) {
    val context = LocalContext.current
    val mapApplier = currentComposer.applier as MapApplier

    // create painter
    val painter = MapMarkerPainter(label = label)

    // convert to bitmap (choose desired px size)
    val density = LocalDensity.current
    val markerSize = with(density) { 60.dp.toPx().toInt() } // adjust size
    val bitmap = remember(label) {
        painterToBitmap(context, painter, markerSize, (markerSize * 1.5f).toInt())
    }

    // add to style
    LaunchedEffect(bitmap) {
        mapApplier.style.addImage(mapImageId, bitmap)
    }

    ComposeNode<SymbolNode, MapApplier>(
        factory = {
            val symbolManager = mapApplier.getOrCreateSymbolManagerForZIndex(0)
            val symbolOptions = SymbolOptions()
                .withLatLng(center)
                .withIconImage(mapImageId)
                .withIconAnchor(Property.ICON_ANCHOR_BOTTOM) // bottom point is pin tip
            val symbol = symbolManager.create(symbolOptions)
            SymbolNode(symbolManager, symbol, onTap = { onTap() }, onLongPress = { onLongPress() })
        },
        update = {
            set(center) {
                symbol.latLng = center
                symbolManager.update(symbol)
            }
        }
    )
}


private fun painterToBitmap(
    context: Context,
    painter: Painter,
    width: Int,
    height: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = androidx.compose.ui.graphics.Canvas(Canvas(bitmap))
    val drawScope = CanvasDrawScope()
    drawScope.draw(
        density = Density(context),
        layoutDirection = LayoutDirection.Ltr,
        canvas = canvas,
        size = Size(width.toFloat(), height.toFloat())
    ) {
        with(painter) { draw(size) }
    }
    return bitmap
}


@Composable
private fun MapMarkerPainter(
    label: String,
    circleSize: Dp = 40.dp,
    tipHeight: Dp = 10.dp,
    bottomPadding: Dp = 20.dp,
    strokeWidth: Dp = 3.dp,
    textSizeSp: Int = 20,
): Painter {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    // precompute px values so painter doesn't need to access LocalDensity each draw
    val circleSizePx = with(density) { circleSize.toPx() }
    val tipHeightPx = with(density) { tipHeight.toPx() }
    val bottomPaddingPx = with(density) { bottomPadding.toPx() }
    val strokeWidthPx = with(density) { strokeWidth.toPx() }
    val triangleHalfPx = with(density) { 7.dp.toPx() } // half-width of the pin base
    val totalHeightPx = circleSizePx + tipHeightPx + bottomPaddingPx

    val textStyle = TextStyle(
        color = Color(0xFF324957),
        fontSize = textSizeSp.sp,
        fontWeight = FontWeight.Bold
    )

    return remember(label, circleSizePx, tipHeightPx, bottomPaddingPx, strokeWidthPx, textSizeSp) {
        object : Painter() {
            override val intrinsicSize: Size
                get() = Size(circleSizePx, totalHeightPx)

            override fun DrawScope.onDraw() {
                // center of circle (x), and y center based on circle size
                val cx = size.width / 2f
                val cy = circleSizePx / 2f
                val circleRadius = circleSizePx / 2f - strokeWidthPx

                // draw circle fill + stroke
                drawCircle(
                    color = Color.White,
                    radius = circleRadius,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = Color(0xFF324957),
                    radius = circleRadius,
                    center = Offset(cx, cy),
                    style = Stroke(width = strokeWidthPx, join = StrokeJoin.Round)
                )

                // measure text to center it inside the circle
                val measured = textMeasurer.measure(
                    text = AnnotatedString(label),
                    style = textStyle,
                    constraints = Constraints(maxWidth = (circleSizePx - strokeWidthPx * 2).toInt())
                )

                // compute top-left so the text is centered in the circle
                val textTopLeft = Offset(
                    x = cx - measured.size.width / 2f,
                    y = cy - measured.size.height / 2f
                )

                // draw measured text
                drawText(textLayoutResult = measured, topLeft = textTopLeft)

                // draw triangular pin tip touching bottom of circle
                val triangleTop = cy + circleRadius
                val p = Path().apply {
                    moveTo(cx - triangleHalfPx, triangleTop)
                    lineTo(cx + triangleHalfPx, triangleTop)
                    lineTo(cx, triangleTop + tipHeightPx)
                    close()
                }
                drawPath(path = p, color = Color(0xFF324957))
            }
        }
    }
}
