package com.surgical.eq.htcne76.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.surgical.eq.htcne76.ui.theme.DarkBorder
import com.surgical.eq.htcne76.ui.theme.DarkSurface
import com.surgical.eq.htcne76.ui.theme.DarkTextPrimary
import com.surgical.eq.htcne76.ui.theme.DarkTextSecondary
import com.surgical.eq.htcne76.ui.theme.NeonCyan

@Composable
fun SavePresetDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var presetName by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface, RoundedCornerShape(10.dp))
                .border(0.7.dp, DarkBorder, RoundedCornerShape(10.dp))
                .padding(14.dp)
        ) {
            Text(
                text = "GUARDAR PRESET QUIRÚRGICO",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = DarkTextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Introduce un nombre para guardar la configuración actual de frecuencias y dinámica.",
                fontSize = 9.5.sp,
                color = DarkTextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = presetName,
                onValueChange = { presetName = it },
                label = { Text("Nombre del Preset", fontSize = 10.sp) },
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 11.sp,
                    color = DarkTextPrimary
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = DarkTextPrimary,
                    unfocusedTextColor = DarkTextPrimary,
                    cursorColor = NeonCyan,
                    focusedLabelColor = NeonCyan,
                    unfocusedLabelColor = DarkTextSecondary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkTextSecondary),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Cancelar", fontSize = 10.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (presetName.isNotBlank()) onSave(presetName.trim())
                    },
                    enabled = presetName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Guardar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
