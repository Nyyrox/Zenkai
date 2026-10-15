package com.nyyrox.aniverse.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.nyyrox.aniverse.ui.theme.OttCardShape
import com.nyyrox.aniverse.ui.theme.OttPillShape
import com.nyyrox.aniverse.ui.theme.OttSkeletonDark
import com.nyyrox.aniverse.ui.theme.OttSkeletonHighlight
import com.nyyrox.aniverse.ui.theme.OttSurface

@Composable
fun rememberShimmerBrush(): Brush {
    val shimmerColors = listOf(
        OttSkeletonDark,
        OttSkeletonHighlight,
        OttSkeletonDark
    )
    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim by transition.animateFloat(
        initialValue = -500f,
        targetValue = 1500f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_anim"
    )
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim, 0f),
        end = Offset(translateAnim + 450f, 450f)
    )
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = OttCardShape
) {
    val brush = rememberShimmerBrush()
    Box(
        modifier = modifier
            .clip(shape)
            .background(brush)
    )
}

@Composable
fun HeroBannerSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Carousel Cards Deck
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(285.dp),
            contentAlignment = Alignment.Center
        ) {
            // Left Card Peek
            ShimmerBox(
                modifier = Modifier
                    .width(155.dp)
                    .height(230.dp)
                    .offset(x = (-115).dp)
                    .alpha(0.4f),
                shape = RoundedCornerShape(16.dp)
            )

            // Right Card Peek
            ShimmerBox(
                modifier = Modifier
                    .width(155.dp)
                    .height(230.dp)
                    .offset(x = 115.dp)
                    .alpha(0.4f),
                shape = RoundedCornerShape(16.dp)
            )

            // Center Primary Card
            ShimmerBox(
                modifier = Modifier
                    .width(185.dp)
                    .height(270.dp),
                shape = RoundedCornerShape(18.dp)
            )
        }

        Spacer(Modifier.height(18.dp))

        // Title Shimmer
        ShimmerBox(
            modifier = Modifier.size(230.dp, 24.dp),
            shape = RoundedCornerShape(6.dp)
        )

        Spacer(Modifier.height(10.dp))

        // Genre Pills Shimmer
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShimmerBox(modifier = Modifier.size(64.dp, 20.dp), shape = OttPillShape)
            ShimmerBox(modifier = Modifier.size(72.dp, 20.dp), shape = OttPillShape)
            ShimmerBox(modifier = Modifier.size(58.dp, 20.dp), shape = OttPillShape)
        }

        Spacer(Modifier.height(18.dp))

        // Action Buttons Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShimmerBox(modifier = Modifier.size(135.dp, 44.dp), shape = OttPillShape)
            ShimmerBox(modifier = Modifier.size(44.dp, 44.dp), shape = OttPillShape)
        }

        Spacer(Modifier.height(16.dp))

        // Page Indicator Dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(5) { i ->
                ShimmerBox(
                    modifier = Modifier.size(if (i == 2) 20.dp else 6.dp, 6.dp),
                    shape = OttPillShape
                )
            }
        }
    }
}

@Composable
fun ContinueWatchingSkeleton() {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        // Section Header Placeholder
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShimmerBox(modifier = Modifier.size(160.dp, 20.dp), shape = RoundedCornerShape(6.dp))
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(4) {
                Column(
                    modifier = Modifier
                        .width(220.dp)
                        .clip(OttCardShape)
                        .background(Color(0x12FFFFFF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(115.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ShimmerBox(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
                        )
                        // Centered play icon outline
                        ShimmerBox(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape
                        )
                    }
                    // Progress bar placeholder
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        shape = RoundedCornerShape(0.dp)
                    )
                    // Episode info
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(14.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                        Spacer(Modifier.height(5.dp))
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .height(11.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PosterRowSkeleton(titleWidth: Int = 130) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        // Section Header Placeholder
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShimmerBox(
                modifier = Modifier.size(titleWidth.dp, 20.dp),
                shape = RoundedCornerShape(6.dp)
            )
            Spacer(Modifier.width(8.dp))
            ShimmerBox(
                modifier = Modifier.size(42.dp, 16.dp),
                shape = RoundedCornerShape(4.dp)
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(5) {
                Column(modifier = Modifier.width(142.dp)) {
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(205.dp),
                        shape = OttCardShape
                    )
                    Spacer(Modifier.height(8.dp))
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(13.dp),
                        shape = RoundedCornerShape(4.dp)
                    )
                    Spacer(Modifier.height(5.dp))
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(11.dp),
                        shape = RoundedCornerShape(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun Top10RowSkeleton() {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShimmerBox(modifier = Modifier.size(150.dp, 20.dp), shape = RoundedCornerShape(6.dp))
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(5) { index ->
                Row(
                    modifier = Modifier.width(185.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Stylized Rank Number Shimmer
                    ShimmerBox(
                        modifier = Modifier
                            .width(48.dp)
                            .height(72.dp)
                            .padding(bottom = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    // Poster Card Shimmer
                    ShimmerBox(
                        modifier = Modifier
                            .width(132.dp)
                            .height(200.dp),
                        shape = OttCardShape
                    )
                }
            }
        }
    }
}

@Composable
fun BrowseGridSkeleton() {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(145.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(8) {
            Column {
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(215.dp),
                    shape = OttCardShape
                )
                Spacer(Modifier.height(8.dp))
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(14.dp),
                    shape = RoundedCornerShape(4.dp)
                )
                Spacer(Modifier.height(5.dp))
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.45f)
                        .height(12.dp),
                    shape = RoundedCornerShape(4.dp)
                )
            }
        }
    }
}

@Composable
fun BrowseListSkeleton() {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(8) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                shape = RoundedCornerShape(14.dp)
            )
        }
    }
}

@Composable
fun AiringListSkeleton() {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(6) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(OttCardShape)
                    .background(OttSurface)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBox(
                    modifier = Modifier.size(70.dp, 95.dp),
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(16.dp),
                        shape = RoundedCornerShape(4.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(14.dp),
                        shape = RoundedCornerShape(4.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(12.dp),
                        shape = RoundedCornerShape(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DetailScreenSkeleton() {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            ) {
                ShimmerBox(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(0.dp)
                )
            }
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 18.dp)) {
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(28.dp),
                    shape = RoundedCornerShape(6.dp)
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShimmerBox(modifier = Modifier.size(60.dp, 24.dp), shape = OttPillShape)
                    ShimmerBox(modifier = Modifier.size(75.dp, 24.dp), shape = OttPillShape)
                    ShimmerBox(modifier = Modifier.size(65.dp, 24.dp), shape = OttPillShape)
                }
                Spacer(Modifier.height(16.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth().height(16.dp), shape = RoundedCornerShape(4.dp))
                Spacer(Modifier.height(6.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth().height(16.dp), shape = RoundedCornerShape(4.dp))
                Spacer(Modifier.height(6.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.6f).height(16.dp), shape = RoundedCornerShape(4.dp))
            }
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 18.dp)) {
                ShimmerBox(modifier = Modifier.size(100.dp, 20.dp), shape = RoundedCornerShape(4.dp))
                Spacer(Modifier.height(12.dp))
                EpisodeGridSkeleton()
            }
        }
    }
}

@Composable
fun EpisodeGridSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(4) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(OttCardShape)
                    .background(OttSurface)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBox(
                    modifier = Modifier.size(110.dp, 68.dp),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.6f).height(15.dp), shape = RoundedCornerShape(4.dp))
                    Spacer(Modifier.height(6.dp))
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.35f).height(12.dp), shape = RoundedCornerShape(4.dp))
                }
            }
        }
    }
}
