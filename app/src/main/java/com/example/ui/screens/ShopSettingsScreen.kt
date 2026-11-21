package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.ShopViewModel
import com.example.ui.components.AppButton
import com.example.ui.components.AppCard
import com.example.ui.components.AppTextField
import com.example.ui.theme.ShopTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopSettingsScreen(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentProfile by viewModel.shopProfile.collectAsStateWithLifecycle()

    var shopName by remember(currentProfile.shopName) { mutableStateOf(currentProfile.shopName) }
    var tagline by remember(currentProfile.tagline) { mutableStateOf(currentProfile.tagline) }
    var phone by remember(currentProfile.contactPhone) { mutableStateOf(currentProfile.contactPhone) }
    var whatsapp by remember(currentProfile.whatsappNumber) { mutableStateOf(currentProfile.whatsappNumber) }
    var address by remember(currentProfile.address) { mutableStateOf(currentProfile.address) }
    var currency by remember(currentProfile.currencySymbol) { mutableStateOf(currentProfile.currencySymbol) }
    var warrantyDays by remember(currentProfile.warrantyDays) { mutableStateOf(currentProfile.warrantyDays.toString()) }
    var terms by remember(currentProfile.termsAndConditions) { mutableStateOf(currentProfile.termsAndConditions) }
    var jobNumberPrefix by remember(currentProfile.jobNumberPrefix) { mutableStateOf(currentProfile.jobNumberPrefix) }
    var preferredWhatsAppPackage by remember(currentProfile.preferredWhatsAppPackage) { mutableStateOf(currentProfile.preferredWhatsAppPackage) }

    BackHandler {
        viewModel.navigateBack()
    }

    val shopColors = ShopTheme.colors
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = shopColors.topBarContent
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = shopColors.topBarContent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = shopColors.topBarContainer)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    AppCard(testTag = "card_settings_profile") {
                        Text(
                            text = stringResource(R.string.settings_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        AppTextField(
                            value = shopName,
                            onValueChange = { shopName = it },
                            label = stringResource(R.string.label_shop_name),
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "input_shop_name"
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        AppTextField(
                            value = tagline,
                            onValueChange = { tagline = it },
                            label = stringResource(R.string.label_shop_tagline),
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "input_shop_tagline"
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = stringResource(R.string.label_shop_phone),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.weight(1f),
                                testTag = "input_shop_phone"
                            )

                            AppTextField(
                                value = whatsapp,
                                onValueChange = { whatsapp = it },
                                label = stringResource(R.string.label_shop_whatsapp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.weight(1f),
                                testTag = "input_shop_whatsapp"
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        AppTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = stringResource(R.string.label_shop_address),
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "input_shop_address"
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppTextField(
                                value = currency,
                                onValueChange = { currency = it },
                                label = stringResource(R.string.label_shop_currency),
                                modifier = Modifier.weight(1f),
                                testTag = "input_shop_currency"
                            )

                            AppTextField(
                                value = warrantyDays,
                                onValueChange = { input -> warrantyDays = input.filter { it.isDigit() } },
                                label = stringResource(R.string.label_shop_warranty),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                testTag = "input_shop_warranty"
                            )
                        }
                    }
                }

                item {
                    AppCard(testTag = "card_settings_job_numbering") {
                        Text(
                            text = stringResource(R.string.section_job_numbering),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        AppTextField(
                            value = jobNumberPrefix,
                            onValueChange = { jobNumberPrefix = it },
                            label = stringResource(R.string.label_job_prefix),
                            placeholder = stringResource(R.string.hint_job_prefix),
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "input_job_prefix"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val previewJobNumber = remember(
                            jobNumberPrefix,
                            currentProfile.lastJobMonth,
                            currentProfile.nextJobNumberSequence
                        ) {
                            val cleanPrefix = jobNumberPrefix.trim()
                                .replace(Regex("\\s+"), "-")
                                .trim('-')
                                .ifBlank { "JOB" }
                            val now = System.currentTimeMillis()
                            val currentMonth = SimpleDateFormat("yyMM", Locale.US).format(Date(now))
                            val todayYymmdd = SimpleDateFormat("yyMMdd", Locale.US).format(Date(now))
                            val nextSeq = if (
                                currentProfile.lastJobMonth == currentMonth &&
                                currentProfile.nextJobNumberSequence >= 1
                            ) {
                                currentProfile.nextJobNumberSequence
                            } else {
                                1
                            }
                            "$cleanPrefix-$todayYymmdd-${String.format(Locale.US, "%03d", nextSeq)}"
                        }
                        Text(
                            text = stringResource(R.string.job_prefix_preview, previewJobNumber),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.job_monthly_reset_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    AppCard(testTag = "card_settings_terms") {
                        Text(
                            text = stringResource(R.string.label_shop_terms),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        AppTextField(
                            value = terms,
                            onValueChange = { terms = it },
                            label = stringResource(R.string.label_shop_terms),
                            singleLine = false,
                            minLines = 4,
                            maxLines = 8,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "input_shop_terms"
                        )
                    }
                }

                item {
                    AppCard(testTag = "card_settings_whatsapp_app") {
                        Text(
                            text = stringResource(R.string.label_whatsapp_app_preference),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.whatsapp_app_pref_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val options = listOf(
                            "auto" to stringResource(R.string.whatsapp_app_auto),
                            "com.whatsapp" to stringResource(R.string.whatsapp_app_standard),
                            "com.whatsapp.w4b" to stringResource(R.string.whatsapp_app_business)
                        )

                        options.forEach { (pkg, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { preferredWhatsAppPackage = pkg }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (preferredWhatsAppPackage == pkg),
                                    onClick = { preferredWhatsAppPackage = pkg },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary,
                                        unselectedColor = MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier.testTag("radio_whatsapp_${pkg.replace(".", "_")}")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                item {
                    AppButton(
                        text = stringResource(R.string.btn_save_settings),
                        onClick = {
                            val parsedWarranty = warrantyDays.trim().toIntOrNull()
                            if (parsedWarranty == null || parsedWarranty < 1) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.error_invalid_warranty),
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@AppButton
                            }
                            val cleanedPrefix = jobNumberPrefix.trim()
                                .replace(Regex("\\s+"), "-")
                                .trim('-')
                                .ifBlank { "JOB" }
                            val updated = currentProfile.copy(
                                shopName = shopName.trim().ifBlank { "Mobile Repair Shop" },
                                tagline = tagline.trim(),
                                contactPhone = phone.trim(),
                                whatsappNumber = whatsapp.trim(),
                                address = address.trim(),
                                currencySymbol = currency.trim().ifBlank { "₹" },
                                warrantyDays = parsedWarranty,
                                termsAndConditions = terms.trim(),
                                jobNumberPrefix = cleanedPrefix,
                                preferredWhatsAppPackage = preferredWhatsAppPackage
                            )
                            viewModel.updateShopProfile(updated) {
                                Toast.makeText(context, context.getString(R.string.toast_settings_saved), Toast.LENGTH_SHORT).show()
                                viewModel.navigateBack()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        testTag = "save_shop_profile_btn",
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null)
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
