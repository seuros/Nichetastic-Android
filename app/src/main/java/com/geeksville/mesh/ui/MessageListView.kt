package com.geeksville.mesh.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.geeksville.mesh.DataPacket
import com.geeksville.mesh.model.Message
import com.geeksville.mesh.ui.components.SimpleAlertDialog
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce

@Composable
internal fun MessageListView(
    messages: List<Message>,
    selectedList: List<Message>,
    onClick: (Message) -> Unit,
    onLongClick: (Message) -> Unit,
    onChipClick: (Message) -> Unit,
    onUnreadChanged: (Long) -> Unit,
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = messages.indexOfLast { !it.read }.coerceAtLeast(0)
    )
    AutoScrollToBottom(listState, messages)
    UpdateUnreadCount(listState, messages, onUnreadChanged)

    var showStatusDialog by remember { mutableStateOf<Message?>(null) }
    if (showStatusDialog != null) {
        val msg = showStatusDialog ?: return
        val (title, text) = msg.getStatusStringRes()
        SimpleAlertDialog(title = title, text = text) { showStatusDialog = null }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        reverseLayout = true,
        // contentPadding = PaddingValues(8.dp)
    ) {
        items(messages, key = { it.uuid }) { msg ->
            val selected by remember { derivedStateOf { selectedList.contains(msg) } }

            MessageItem(
                shortName = msg.user.shortName.takeIf { msg.user.id != DataPacket.ID_LOCAL },
                messageText = msg.text,
                messageTime = msg.time,
                messageStatus = msg.status,
                selected = selected,
                onClick = { onClick(msg) },
                onLongClick = { onLongClick(msg) },
                onChipClick = { onChipClick(msg) },
                onStatusClick = { showStatusDialog = msg }
            )
        }
    }
}

@Composable
private fun <T> AutoScrollToBottom(
    listState: LazyListState,
    list: List<T>,
    itemThreshold: Int = 3,
) = with(listState) {
    val shouldAutoScroll by remember { derivedStateOf { firstVisibleItemIndex < itemThreshold } }
    if (shouldAutoScroll) {
        LaunchedEffect(list) {
            if (!isScrollInProgress) {
                scrollToItem(0)
            }
        }
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun UpdateUnreadCount(
    listState: LazyListState,
    messages: List<Message>,
    onUnreadChanged: (Long) -> Unit,
) {
    val unreadIndex by remember { derivedStateOf { messages.indexOfLast { !it.read } } }
    val firstVisibleItemIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }

    if (unreadIndex != -1 && firstVisibleItemIndex != -1 && firstVisibleItemIndex <= unreadIndex) {
        LaunchedEffect(firstVisibleItemIndex, unreadIndex) {
            snapshotFlow { listState.firstVisibleItemIndex }
                .debounce(timeoutMillis = 500L)
                .collectLatest { index ->
                    val lastVisibleItem = messages[index]
                    onUnreadChanged(lastVisibleItem.receivedTime)
                }
        }
    }
}
