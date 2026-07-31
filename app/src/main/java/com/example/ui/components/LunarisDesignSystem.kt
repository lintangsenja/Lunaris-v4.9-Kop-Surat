package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.unit.sp

import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import com.example.ui.theme.HeaderGradientBrush
import com.example.ui.theme.HeaderTitleColor
import com.example.ui.theme.HeaderSubtitleColor

data class FilterGroup(
    val title: String,
    val options: List<String>,
    val selectedOption: String,
    val onOptionSelected: (String) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LunarisFilterDialog(
    onDismissRequest: () -> Unit,
    title: String = "Filter Data",
    filterGroups: List<FilterGroup>,
    onReset: () -> Unit,
    onApply: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = true),
        modifier = Modifier.clip(RoundedCornerShape(20.dp))
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF3E8FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = Color(0xFF7C3AED),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1E293B)
                        )
                    }
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Vertical Column Form Fields with Labels Above Dropdowns
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    filterGroups.forEach { group ->
                        FilterGroupDropdownSelector(group = group)
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onReset,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF475569)),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text("Reset", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }

                    Button(
                        onClick = onApply,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED), contentColor = Color.White),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(44.dp)
                            .testTag("filter_btn_terapkan")
                    ) {
                        Text("Terapkan Filter", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@Composable
fun LunarisHeader(
    title: String,
    subtitle: String? = null,
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            .background(HeaderGradientBrush)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onNavigateBack != null) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("btn_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = HeaderTitleColor
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = HeaderTitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.5.sp
                        ),
                        color = HeaderSubtitleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            if (actions != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    actions()
                }
            }
        }
    }
}

@Composable
fun LunarisCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    colors: CardColors = CardDefaults.cardColors(containerColor = Color.White),
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border: BorderStroke? = BorderStroke(1.dp, Color(0xFFCBD5E1)),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        content = content
    )
}

@Composable
fun LunarisCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(16.dp),
    colors: CardColors = CardDefaults.cardColors(containerColor = Color.White),
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border: BorderStroke? = BorderStroke(1.dp, Color(0xFFCBD5E1)),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun LunarisTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = TextStyle.Default,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    isStaticOutline: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    shape: Shape = RoundedCornerShape(12.dp),
    colors: TextFieldColors? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val focusedBorderColor = Color(0xFF7C3AED)
    val unfocusedBorderColor = Color(0xFFCBD5E1)
    
    val containerColor = Color.White

    val standardizedColors = colors ?: OutlinedTextFieldDefaults.colors(
        focusedBorderColor = focusedBorderColor,
        unfocusedBorderColor = unfocusedBorderColor,
        focusedLabelColor = focusedBorderColor,
        unfocusedLabelColor = Color.Gray,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        cursorColor = focusedBorderColor,
        focusedSupportingTextColor = focusedBorderColor,
        focusedContainerColor = containerColor,
        unfocusedContainerColor = containerColor,
        disabledContainerColor = containerColor,
        errorContainerColor = containerColor
    )

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        supportingText = supportingText,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        shape = shape,
        colors = standardizedColors,
        interactionSource = interactionSource
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LunarisTwoColumnFilterDialog(
    onDismissRequest: () -> Unit,
    title: String = "Filter Data",
    leftColumnGroups: List<FilterGroup>,
    rightColumnGroups: List<FilterGroup>,
    onReset: () -> Unit,
    onApply: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = true),
        modifier = Modifier.clip(RoundedCornerShape(16.dp))
    ) {
        LunarisCard(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = Color(0xFFF3E8FF),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = Color(0xFF7C3AED),
                                modifier = Modifier
                                    .size(28.dp)
                                    .padding(5.dp)
                            )
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1E293B)
                        )
                    }
                    IconButton(onClick = onReset) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Filter",
                            tint = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Side-by-side Grid Layout (Two Columns)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left Column
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        leftColumnGroups.forEach { group ->
                            FilterGroupDropdownSelector(group = group)
                        }
                    }

                    // Right Column
                    if (rightColumnGroups.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rightColumnGroups.forEach { group ->
                                FilterGroupDropdownSelector(group = group)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onReset,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF475569)),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Text("Reset", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }

                    Button(
                        onClick = onApply,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED), contentColor = Color.White),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(42.dp)
                            .testTag("filter_btn_terapkan")
                    ) {
                        Text("Terapkan Filter", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterGroupDropdownSelector(group: FilterGroup) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = group.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
            color = Color(0xFF334155),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
                onClick = { expanded = true },
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, if (!group.selectedOption.startsWith("Semua")) Color(0xFF7C3AED) else Color(0xFFCBD5E1)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("filter_dropdown_${group.title}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = group.selectedOption,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = if (!group.selectedOption.startsWith("Semua")) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (!group.selectedOption.startsWith("Semua")) Color(0xFF7C3AED) else Color(0xFF475569),
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 220.dp)
            ) {
                group.options.forEach { option ->
                    val isSelected = option == group.selectedOption
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF7C3AED) else Color(0xFF334155)
                            )
                        },
                        onClick = {
                            group.onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LunarisDatePickerDialog(
    onDismissRequest: () -> Unit,
    selectedDateString: String,
    onDateSelected: (String) -> Unit,
    title: String = "Pilih Tanggal",
    confirmButtonText: String = "Simpan Tanggal"
) {
    val todayCal = java.util.Calendar.getInstance()
    val parts = selectedDateString.split("-")
    val initialYear = parts.getOrNull(0)?.toIntOrNull() ?: todayCal.get(java.util.Calendar.YEAR)
    val initialMonth = parts.getOrNull(1)?.toIntOrNull() ?: (todayCal.get(java.util.Calendar.MONTH) + 1)
    val initialDay = parts.getOrNull(2)?.toIntOrNull() ?: todayCal.get(java.util.Calendar.DAY_OF_MONTH)

    var tempYear by remember { androidx.compose.runtime.mutableIntStateOf(initialYear) }
    var tempMonth by remember { androidx.compose.runtime.mutableIntStateOf(initialMonth) }
    var tempDay by remember { androidx.compose.runtime.mutableIntStateOf(initialDay) }

    var monthExpanded by remember { mutableStateOf(false) }
    var yearExpanded by remember { mutableStateOf(false) }

    val maxDaysInMonth = remember(tempYear, tempMonth) {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.YEAR, tempYear)
        cal.set(java.util.Calendar.MONTH, (tempMonth - 1).coerceIn(0, 11))
        cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    }

    val monthsList = listOf(
        "01 - Januari", "02 - Februari", "03 - Maret", "04 - April",
        "05 - Mei", "06 - Juni", "07 - Juli", "08 - Agustus",
        "09 - September", "10 - Oktober", "11 - November", "12 - Desember"
    )

    val currentYear = todayCal.get(java.util.Calendar.YEAR)
    val yearRange = (currentYear - 5)..(currentYear + 5)

    fun formatDateDisplay(y: Int, m: Int, d: Int): String {
        return try {
            val cal = java.util.Calendar.getInstance()
            cal.set(y, m - 1, d)
            val sdf = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale("id", "ID"))
            sdf.format(cal.time)
        } catch (e: Exception) {
            "$d-$m-$y"
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = true),
        modifier = Modifier.clip(RoundedCornerShape(20.dp))
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = Color(0xFF7C3AED),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF1E293B)
                        )
                    }

                    Surface(
                        onClick = {
                            val now = java.util.Calendar.getInstance()
                            tempYear = now.get(java.util.Calendar.YEAR)
                            tempMonth = now.get(java.util.Calendar.MONTH) + 1
                            tempDay = now.get(java.util.Calendar.DAY_OF_MONTH)
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFF3E8FF),
                        border = BorderStroke(1.dp, Color(0xFFDDD6FE))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Today,
                                contentDescription = null,
                                tint = Color(0xFF7C3AED),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Hari Ini",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7C3AED)
                            )
                        }
                    }
                }

                // Selected Date Card
                val safeDay = tempDay.coerceAtMost(maxDaysInMonth)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E8FF)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, Color(0xFFDDD6FE))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = Color(0xFF7C3AED),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Tanggal Terpilih",
                                fontSize = 10.sp,
                                color = Color(0xFF6B21A8),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = formatDateDisplay(tempYear, tempMonth, safeDay),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF581C87)
                            )
                        }
                    }
                }

                // Dropdown Selectors for Month and Year
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Month Dropdown
                    Box(modifier = Modifier.weight(1.4f)) {
                        Surface(
                            onClick = { monthExpanded = true },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Bulan", fontSize = 9.sp, color = Color(0xFF64748B))
                                    Text(
                                        text = monthsList.getOrElse(tempMonth - 1) { "01 - Januari" },
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1E293B)
                                    )
                                }
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = monthExpanded,
                            onDismissRequest = { monthExpanded = false },
                            modifier = Modifier.heightIn(max = 200.dp)
                        ) {
                            monthsList.forEachIndexed { idx, mLabel ->
                                DropdownMenuItem(
                                    text = { Text(mLabel, fontSize = 12.sp) },
                                    onClick = {
                                        tempMonth = idx + 1
                                        monthExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Year Dropdown
                    Box(modifier = Modifier.weight(1f)) {
                        Surface(
                            onClick = { yearExpanded = true },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Tahun", fontSize = 9.sp, color = Color(0xFF64748B))
                                    Text(
                                        text = tempYear.toString(),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1E293B)
                                    )
                                }
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = yearExpanded,
                            onDismissRequest = { yearExpanded = false },
                            modifier = Modifier.heightIn(max = 200.dp)
                        ) {
                            yearRange.forEach { y ->
                                DropdownMenuItem(
                                    text = { Text(y.toString(), fontSize = 12.sp) },
                                    onClick = {
                                        tempYear = y
                                        yearExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Day Grid Picker
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Pilih Hari (1 - $maxDaysInMonth):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569)
                    )

                    val totalDays = maxDaysInMonth
                    val rows = (totalDays + 6) / 7
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (r in 0 until rows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (c in 0 until 7) {
                                    val dayNum = r * 7 + c + 1
                                    if (dayNum <= totalDays) {
                                        val isSelected = dayNum == safeDay
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .background(
                                                    color = if (isSelected) Color(0xFF7C3AED) else Color(0xFFF1F5F9),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable { tempDay = dayNum },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = dayNum.toString(),
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color.White else Color(0xFF334155)
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Text("Batal", color = Color(0xFF64748B), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            val validDay = tempDay.coerceAtMost(maxDaysInMonth)
                            val formattedDate = String.format(java.util.Locale.US, "%04d-%02d-%02d", tempYear, tempMonth, validDay)
                            onDateSelected(formattedDate)
                            onDismissRequest()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED), contentColor = Color.White),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(42.dp)
                    ) {
                        Text(confirmButtonText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
