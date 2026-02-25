package com.viwath.practice_module_app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HalfDefaultBottomSheetFullContentWhenHidden() {
    val scope = rememberCoroutineScope()

    val configuration = LocalConfiguration.current
    val halfScreenDp = (configuration.screenHeightDp / 2).dp

    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded, // start visible
        skipHiddenState = false                      // ✅ allow hide()
    )

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = sheetState
    )

    // ✅ When hidden => 0 peek so MainContent becomes full screen
    val peekHeight = if (sheetState.currentValue == SheetValue.Hidden) 0.dp else halfScreenDp

    BottomSheetScaffold(
        sheetSwipeEnabled = false, //sheetSwipeEnabled = false, fixed sheet (no drag)
        scaffoldState = scaffoldState,
        sheetPeekHeight = peekHeight,
        sheetContent = {
            SheetContent(
                onHide = { scope.launch { sheetState.hide() } },
                onHalf = { scope.launch { sheetState.partialExpand() } },
                onExpand = { scope.launch { sheetState.expand() } }
            )
        },
        containerColor = Color.Blue
    ) { innerPadding ->
        MainContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}

@Composable
private fun MainContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color.Transparent)
            .padding(16.dp)
    ) {
        Text(
            "MainContent (should be FULL when sheet hidden)",
            style = MaterialTheme.typography.headlineSmall
        )
    }
}

@Composable
private fun SheetContent(
    onHide: () -> Unit,
    onHalf: () -> Unit,
    onExpand: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp)
            .padding(16.dp)
    ) {
        // handle
        Box(
            Modifier
                .width(48.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.LightGray.copy(alpha = 0.4f))
                .align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(12.dp))

        Text("Bottom Sheet", style = MaterialTheme.typography.titleLarge)

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onExpand) { Text("Expand") }
            OutlinedButton(onClick = onHalf) { Text("Half") }
            OutlinedButton(onClick = onHide) { Text("Hide") }
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(20) { i ->
                ListItem(
                    headlineContent = { Text("Item #$i") },
                    supportingContent = { Text("Details...") }
                )
                HorizontalDivider()
            }
        }
    }
}

