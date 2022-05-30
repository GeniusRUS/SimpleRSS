package com.genius.srss.util

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.genius.srss.R
import com.genius.srss.ui.theme.SRSSTheme

@Composable
fun Tutorial(
    toClose: () -> Unit,
    modifier: Modifier = Modifier
) {

    val tips = listOf(
        TutorialView.Tip(
            R.string.tutorial_pinch_zoom_in,
            R.drawable.ic_vector_pinch
        ),
        TutorialView.Tip(
            R.string.tutorial_pinch_zoom_out,
            R.drawable.ic_vector_pinch
        ),
        TutorialView.Tip(
            R.string.tutorial_assign_subscription_to_folder,
            R.drawable.ic_vector_touch_app
        ),
        TutorialView.Tip(
            R.string.tutorial_create_folder_or_subscription,
            R.drawable.ic_vector_add_circle_outline
        ),
        TutorialView.Tip(
            R.string.tutorial_remove_subscription,
            R.drawable.ic_vector_swipe_left
        ),
        TutorialView.Tip(
            R.string.tutorial_unlink_subscription,
            R.drawable.ic_vector_swipe_right
        ),
        TutorialView.Tip(
            R.string.tutorial_manual_folder_sorting,
            R.drawable.ic_vector_touch_app
        ),
        TutorialView.Tip(
            R.string.tutorial_manual_folder_mode,
            R.drawable.ic_vector_list
        )
    )

    var state by remember { mutableStateOf(tips.first()) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable {
                val currentPosition = tips.indexOf(state)
                val nextItemOrClose = tips.getOrNull(currentPosition + 1)
                if (nextItemOrClose != null) {
                    state = nextItemOrClose
                } else {
                    toClose.invoke()
                }
            }
    ) {
        Crossfade(
            targetState = state,
            modifier = Modifier
                .align(Alignment.Center)
        ) { state ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center,
                    text = stringResource(id = state.message),
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                )
                Icon(
                    painter = painterResource(id = state.icon),
                    contentDescription = stringResource(id = R.string.content_description_image),
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(24.dp)
                )
            }
        }
        AnimatedVisibility(
            visible = state != tips.first(),
            modifier = Modifier
                .align(Alignment.BottomStart)
        ) {
            IconButton(
                onClick = {
                    val currentPosition = tips.indexOf(state)
                    state = tips.getOrElse(currentPosition - 1) {
                        tips.first()
                    }
                },
                modifier = Modifier
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_vector_chevron_left),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        IconButton(
            onClick = toClose,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopEnd)
                .statusBarsPadding()
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_vector_close),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO)
@Composable
fun TutorialPreview() {
    SRSSTheme {
        Tutorial(
            toClose = {}
        )
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun TutorialDarkPreview() {
    SRSSTheme {
        Tutorial(
            toClose = {}
        )
    }
}