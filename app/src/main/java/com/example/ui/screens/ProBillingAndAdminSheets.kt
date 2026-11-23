package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.BillingConfigDoc
import com.example.model.PakistanPaymentMethod
import com.example.model.ProEntitlementDoc
import com.example.model.ProPackageTier
import com.example.model.ProPurchaseRequestDoc
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonRose
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletPulse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProPurchaseSheet(
    billingConfig: BillingConfigDoc,
    entitlement: ProEntitlementDoc?,
    hasProAccess: Boolean,
    myRequests: List<ProPurchaseRequestDoc>,
    isBusy: Boolean,
    onSubmitPayment: (
        tier: ProPackageTier,
        method: PakistanPaymentMethod,
        senderPhone: String,
        transactionId: String
    ) -> Unit,
    onShowMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    var selectedTier by remember { mutableStateOf(ProPackageTier.MONTHLY_PRO) }
    var selectedMethod by remember { mutableStateOf(PakistanPaymentMethod.JAZZCASH) }
    var senderPhone by remember { mutableStateOf("") }
    var transactionId by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val receiverNumber = billingConfig.numberFor(selectedMethod)
    val receiverTitle = billingConfig.titleFor(selectedMethod)
    val selectedPricePkr = billingConfig.priceFor(selectedTier)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StudioSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(StudioAmber.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.WorkspacePremium, null, tint = StudioAmber, modifier = Modifier.size(22.dp))
                    }
                    Column {
                        Text(
                            text = "s.s Studio AI Pro Packages",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = TextPrimary
                        )
                        Text(
                            text = "Pay securely via JazzCash or EasyPaisa (PKR)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Active Pro Banner
            if (hasProAccess) {
                val activeTier = ProPackageTier.fromId(entitlement?.tier ?: ProPackageTier.YEARLY_PRO.id)
                Surface(
                    color = StudioEmerald.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, StudioEmerald),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.Verified, null, tint = StudioEmerald, modifier = Modifier.size(22.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AI Video Pro Active (${activeTier.title})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = StudioEmerald
                            )
                            Text(
                                text = "Unlimited 1080P+ AI Video Generation, CapCut Pro FX & Canva Pro unlocked.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Step 1: Select Package Tier
            Text("1. Choose Pro Package", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProPackageTier.entries.forEach { tier ->
                    val active = selectedTier == tier
                    val price = billingConfig.priceFor(tier)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pro_pkg_${tier.id.lowercase()}")
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { selectedTier = tier },
                        color = if (active) ElectricCyan.copy(alpha = 0.14f) else StudioSurfaceElevated,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, if (active) ElectricCyan else StudioBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = tier.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                    Surface(
                                        color = StudioAmber.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = tier.badge,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = StudioAmber,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = tier.tagline,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                text = "Rs. $price",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (active) ElectricCyan else StudioAmber
                            )
                        }
                    }
                }
            }

            // Step 2: Select JazzCash or EasyPaisa
            Text("2. Select Payment Wallet", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PakistanPaymentMethod.entries.forEach { method ->
                    val active = selectedMethod == method
                    val accent = Color(method.accentHex)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pro_method_${method.id.lowercase()}")
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedMethod = method },
                        color = if (active) accent.copy(alpha = 0.18f) else StudioSurfaceElevated,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (active) accent else StudioBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Filled.AccountBalanceWallet, null, tint = accent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = method.displayName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (active) accent else TextPrimary
                            )
                        }
                    }
                }
            }

            // Admin's Live Merchant Number Card + Copy Button
            val methodAccent = Color(selectedMethod.accentHex)
            Surface(
                color = StudioSurfaceElevated,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, methodAccent.copy(alpha = 0.65f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Send Rs. $selectedPricePkr to ${selectedMethod.displayName}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = methodAccent
                            )
                            Text(
                                text = receiverNumber.ifBlank { "Contact Admin for number" },
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = TextPrimary
                            )
                            Text(
                                text = "Account Title: $receiverTitle",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        IconButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(receiverNumber))
                                onShowMessage("${selectedMethod.displayName} number copied!")
                            },
                            modifier = Modifier
                                .testTag("pro_copy_number_btn")
                                .clip(CircleShape)
                                .background(methodAccent.copy(alpha = 0.16f))
                        ) {
                            Icon(Icons.Filled.ContentCopy, "Copy number", tint = methodAccent)
                        }
                    }
                    if (billingConfig.adminNote.isNotBlank()) {
                        Text(
                            text = billingConfig.adminNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }

            // Step 3: Submit Payment Proof (Sender Number + TID)
            Text("3. Enter Payment Confirmation", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            OutlinedTextField(
                value = senderPhone,
                onValueChange = {
                    senderPhone = it
                    localError = null
                },
                label = { Text("Your ${selectedMethod.displayName} Mobile Number") },
                placeholder = { Text("0300 1234567") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pro_sender_phone_input")
            )
            OutlinedTextField(
                value = transactionId,
                onValueChange = {
                    transactionId = it
                    localError = null
                },
                label = { Text("Transaction ID (TID / Trx ID)") },
                placeholder = { Text("e.g. 84920194821") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pro_tid_input")
            )

            localError?.let { err ->
                Text(err, style = MaterialTheme.typography.labelMedium, color = NeonRose)
            }

            Button(
                onClick = {
                    if (senderPhone.trim().length < 7) {
                        localError = "Enter your JazzCash / EasyPaisa mobile number."
                    } else if (transactionId.trim().length < 4) {
                        localError = "Enter the Transaction ID (TID) from your payment receipt."
                    } else {
                        onSubmitPayment(selectedTier, selectedMethod, senderPhone, transactionId)
                    }
                },
                enabled = !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("pro_submit_payment_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = methodAccent,
                    contentColor = Color.White
                )
            ) {
                if (isBusy) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Submit Payment for Admin Approval", fontWeight = FontWeight.ExtraBold)
            }

            // My Payment History / Approval Status
            if (myRequests.isNotEmpty()) {
                Text(
                    text = "My Payment Requests (${myRequests.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    myRequests.forEach { req ->
                        val statusColor = when (req.status) {
                            "APPROVED" -> StudioEmerald
                            "REJECTED" -> NeonRose
                            else -> StudioAmber
                        }
                        Surface(
                            color = StudioSurfaceElevated,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${req.packageTitle} · Rs. ${req.amountPkr}",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                    Surface(
                                        color = statusColor.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = req.status,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = statusColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${req.paymentMethod} · From ${req.senderPhone} · TID: ${req.transactionId}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                if (req.adminNote.isNotBlank()) {
                                    Text(
                                        text = "Admin Note: ${req.adminNote}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = statusColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBillingConsoleSheet(
    billingConfig: BillingConfigDoc,
    allRequests: List<ProPurchaseRequestDoc>,
    isBusy: Boolean,
    onSaveBillingConfig: (
        jazzCashNumber: String,
        jazzCashTitle: String,
        easyPaisaNumber: String,
        easyPaisaTitle: String,
        weeklyPricePkr: Long,
        monthlyPricePkr: Long,
        yearlyPricePkr: Long,
        adminNote: String
    ) -> Unit,
    onReviewRequest: (request: ProPurchaseRequestDoc, approve: Boolean, adminNote: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) }

    var jazzNumber by remember(billingConfig) { mutableStateOf(billingConfig.jazzCashNumber) }
    var jazzTitle by remember(billingConfig) { mutableStateOf(billingConfig.jazzCashTitle) }
    var easyNumber by remember(billingConfig) { mutableStateOf(billingConfig.easyPaisaNumber) }
    var easyTitle by remember(billingConfig) { mutableStateOf(billingConfig.easyPaisaTitle) }
    var weeklyPkrText by remember(billingConfig) { mutableStateOf(billingConfig.weeklyPricePkr.toString()) }
    var monthlyPkrText by remember(billingConfig) { mutableStateOf(billingConfig.monthlyPricePkr.toString()) }
    var yearlyPkrText by remember(billingConfig) { mutableStateOf(billingConfig.yearlyPricePkr.toString()) }
    var adminNoteText by remember(billingConfig) { mutableStateOf(billingConfig.adminNote) }

    val pendingCount = remember(allRequests) { allRequests.count { it.status == "PENDING" } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StudioSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(VioletPulse.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.AdminPanelSettings, null, tint = VioletPulse, modifier = Modifier.size(22.dp))
                }
                Column {
                    Text(
                        text = "Single-App Admin Console",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = TextPrimary
                    )
                    Text(
                        text = "Manage JazzCash / EasyPaisa payout numbers & approve Pro orders",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // Admin Sub-Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    0 to "Payout Numbers",
                    1 to "Approvals ($pendingCount Pending)"
                ).forEach { (idx, label) ->
                    val active = selectedTab == idx
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_tab_$idx")
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedTab = idx },
                        color = if (active) VioletPulse else StudioSurfaceElevated,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (active) VioletPulse else StudioBorder)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (active) Color.White else TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            if (selectedTab == 0) {
                OutlinedTextField(
                    value = jazzNumber,
                    onValueChange = { jazzNumber = it },
                    label = { Text("JazzCash Receiver Number") },
                    placeholder = { Text("+92 300 1234567") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_jazzcash_number_input")
                )
                OutlinedTextField(
                    value = jazzTitle,
                    onValueChange = { jazzTitle = it },
                    label = { Text("JazzCash Account Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = easyNumber,
                    onValueChange = { easyNumber = it },
                    label = { Text("EasyPaisa Receiver Number") },
                    placeholder = { Text("+92 345 1234567") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_easypaisa_number_input")
                )
                OutlinedTextField(
                    value = easyTitle,
                    onValueChange = { easyTitle = it },
                    label = { Text("EasyPaisa Account Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = weeklyPkrText,
                        onValueChange = { weeklyPkrText = it.filter(Char::isDigit) },
                        label = { Text("Weekly Rs.") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = monthlyPkrText,
                        onValueChange = { monthlyPkrText = it.filter(Char::isDigit) },
                        label = { Text("Monthly Rs.") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = yearlyPkrText,
                        onValueChange = { yearlyPkrText = it.filter(Char::isDigit) },
                        label = { Text("Yearly Rs.") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = adminNoteText,
                    onValueChange = { adminNoteText = it },
                    label = { Text("Checkout Instructions for Buyers") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        onSaveBillingConfig(
                            jazzNumber,
                            jazzTitle,
                            easyNumber,
                            easyTitle,
                            weeklyPkrText.toLongOrNull() ?: 299L,
                            monthlyPkrText.toLongOrNull() ?: 799L,
                            yearlyPkrText.toLongOrNull() ?: 2499L,
                            adminNoteText
                        )
                    },
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("admin_save_billing_btn"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color(0xFF041418))
                ) {
                    Icon(Icons.Filled.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Live Admin Numbers & Prices", fontWeight = FontWeight.ExtraBold)
                }
            } else {
                if (allRequests.isEmpty()) {
                    Surface(
                        color = StudioSurfaceElevated,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, StudioBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No Pro package purchase requests submitted yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        allRequests.forEach { req ->
                            val statusColor = when (req.status) {
                                "APPROVED" -> StudioEmerald
                                "REJECTED" -> NeonRose
                                else -> StudioAmber
                            }
                            Surface(
                                color = StudioSurfaceElevated,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.55f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${req.buyerName} (${req.buyerHandle})",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = TextPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Surface(
                                            color = statusColor.copy(alpha = 0.16f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = req.status,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = statusColor,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                    if (req.buyerEmail.isNotBlank()) {
                                        Text(req.buyerEmail, style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                                    }
                                    Text(
                                        text = "${req.packageTitle} · Rs. ${req.amountPkr} via ${req.paymentMethod}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = StudioAmber
                                    )
                                    Text(
                                        text = "Sender: ${req.senderPhone} · TID: ${req.transactionId} -> To: ${req.receiverNumber}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    if (req.status != "APPROVED") {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { onReviewRequest(req, true, "Verified & Pro Activated") },
                                                enabled = !isBusy,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("admin_approve_${req.id}"),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = StudioEmerald,
                                                    contentColor = Color(0xFF041418)
                                                ),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Approve & Unlock Pro", fontWeight = FontWeight.Bold)
                                            }
                                            OutlinedButton(
                                                onClick = { onReviewRequest(req, false, "TID not found in statement") },
                                                enabled = !isBusy,
                                                border = BorderStroke(1.dp, NeonRose),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Filled.Close, null, tint = NeonRose, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Reject", color = NeonRose)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}
