package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.MetricStatCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminDashboardScreen(
    allStudents: List<StudentProfile>,
    allNotices: List<Notice>,
    latestWorkoutPlan: DailyWorkoutPlan?,
    onNavigate: (String) -> Unit,
    onAddStudent: (StudentProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddStudentDialog by remember { mutableStateOf(false) }
    var selectedStudentForDetails by remember { mutableStateOf<StudentProfile?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Admin Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(OliveTertiary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = "Admin",
                                    tint = OliveTertiary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "प्रशिक्षक नियंत्रण कक्ष (Admin Panel)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "जय बजरंग अखाड़ा, मौरिकला गुफा",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            color = OliveTertiary,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "ADMIN",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        // Summary Metric Stats
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricStatCard(
                    title = "कुल छात्र (Students)",
                    value = "${allStudents.size}",
                    subtitle = "पंजीकृत अभ्यर्थी",
                    icon = Icons.Default.Groups,
                    iconColor = SaffronPrimary,
                    modifier = Modifier.weight(1f)
                )

                MetricStatCard(
                    title = "सक्रिय सूचनाएं",
                    value = "${allNotices.size}",
                    subtitle = "नोटिस बोर्ड",
                    icon = Icons.Default.Campaign,
                    iconColor = OliveTertiary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Admin Management Actions
        item {
            Text(
                text = "प्रशिक्षक कार्य एवं प्रबंधन (Admin Actions)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminActionRow(
                    title = "प्रश्न बैंक प्रबंधन (Question Bank System)",
                    subtitle = "विषय, टॉपिक, प्रश्न जोड़ें/संपादित करें एवं एक्टिवेशन नियंत्रित करें",
                    icon = Icons.Default.Quiz,
                    color = SaffronPrimary,
                    onClick = { onNavigate("admin_question_bank") }
                )

                AdminActionRow(
                    title = "दैनिक ग्राउंड उपस्थिति दर्ज करें (Mark Attendance)",
                    subtitle = "सभी छात्रों की एक साथ उपस्थिति लगाएं",
                    icon = Icons.Default.FactCheck,
                    color = OliveTertiary,
                    onClick = { onNavigate("admin_attendance") }
                )

                AdminActionRow(
                    title = "दैनिक वर्कआउट प्लान जारी करें (Daily Workout Plan)",
                    subtitle = "रनिंग KM, पुश-अप्स, सिट-अप्स व निर्देश सेट करें",
                    icon = Icons.Default.SportsScore,
                    color = SaffronPrimary,
                    onClick = { onNavigate("admin_workout") }
                )

                AdminActionRow(
                    title = "नोटिस व भर्ती सूचना जारी करें (Post Notice & Jobs)",
                    subtitle = "अखाड़ा नोटिस एवं सरकारी भर्ती अपडेट्स जोड़ें",
                    icon = Icons.Default.Campaign,
                    color = NavySecondary,
                    onClick = { onNavigate("admin_notices") }
                )

                AdminActionRow(
                    title = "नया छात्र पंजीयन करें (Enroll New Student)",
                    subtitle = "यूनिक JBA आईडी, ऑटो-आयु, शारीरिक माप व लक्ष्य दर्ज करें",
                    icon = Icons.Default.PersonAdd,
                    color = Color(0xFF7C3AED),
                    onClick = { showAddStudentDialog = true }
                )

                AdminActionRow(
                    title = "अखाड़ा कंटेंट प्रबंधन (Content CMS)",
                    subtitle = "प्रशिक्षक, गैलरी फोटो, सफलता की कहानियां व संपर्क विवरण प्रबंधित करें",
                    icon = Icons.Default.EditNote,
                    color = SaffronDark,
                    onClick = { onNavigate("admin_content_cms") }
                )

                AdminActionRow(
                    title = "एडमिन सुरक्षा व पिन प्रबंधन (Admin Security)",
                    subtitle = "सुरक्षा पिन बदलें एवं एन्क्रिप्टेड क्रेडेंशियल्स प्रबंधित करें",
                    icon = Icons.Default.Security,
                    color = Color(0xFF00695C),
                    onClick = { onNavigate("admin_security") }
                )
            }
        }

        // Active Daily Workout Plan Preview
        item {
            latestWorkoutPlan?.let { plan ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "वर्तमान सक्रिय ट्रेनिंग प्लान",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = { onNavigate("admin_workout") },
                                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("बदलें", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(text = plan.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(text = plan.instructions, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "रनिंग: ${plan.targetRunningKm} KM | पुश-अप्स: ${plan.targetPushups} | सिट-अप्स: ${plan.targetSitups} | बीम: ${plan.targetPullups}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SaffronPrimary
                        )
                    }
                }
            }
        }

        // Enrolled Students List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "नामांकित छात्र सूची (Registered Students - ${allStudents.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "प्रोफाइल देखने हेतु टैप करें",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(allStudents) { student ->
            val calculatedAge = remember(student.dob) {
                val calc = com.example.util.ProfileUtils.calculateAgeFromDob(student.dob)
                if (calc > 0) calc else student.age
            }

            Card(
                onClick = { selectedStudentForDetails = student },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_student_card_${student.studentId}"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = SaffronContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = student.studentId,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSaffronContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = student.fullName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "गाँव: ${student.village} • उम्र: ${calculatedAge} वर्ष • लक्ष्य: ${student.recruitmentGoal}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = StatusPresent.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "उपस्थिति: ${student.attendanceStreakDays} दिन लगातार",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = StatusPresent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Surface(
                                color = OliveTertiary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "स्कोर: ${student.overallScore}/100",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = OliveTertiary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "View Profile",
                        tint = SaffronPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }

    // Complete Student Profile Viewer Dialog for Admin
    if (selectedStudentForDetails != null) {
        val s = selectedStudentForDetails!!
        val studentAge = remember(s.dob) {
            val calc = com.example.util.ProfileUtils.calculateAgeFromDob(s.dob)
            if (calc > 0) calc else s.age
        }
        val bmi = remember(s.heightCm, s.weightKg) {
            com.example.util.ProfileUtils.getBmiCategory(s.heightCm, s.weightKg)
        }

        AlertDialog(
            onDismissRequest = { selectedStudentForDetails = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Badge, contentDescription = null, tint = SaffronPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "छात्र संपूर्ण प्रोफाइल (Student Profile)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = s.fullName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text(text = "आईडी: ${s.studentId} • पिता: ${s.fatherName}", style = MaterialTheme.typography.bodySmall)
                                Text(text = "मोबाइल: ${s.mobileNumber} • गाँव: ${s.village}", style = MaterialTheme.typography.bodySmall)
                                Text(text = "जन्म तिथि: ${s.dob} • आयु: $studentAge वर्ष • लिंग: ${s.gender}", style = MaterialTheme.typography.bodySmall)
                                Text(text = "शिक्षा: ${s.education} • लक्ष्य: ${s.recruitmentGoal}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = SaffronPrimary)
                                Text(text = "नामांकन: ${s.joinDate}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    item {
                        Text("शारीरिक मापदंड व बीएमआई", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ऊंचाई: ${s.heightCm} cm", style = MaterialTheme.typography.bodySmall)
                            Text("वजन: ${s.weightKg} kg", style = MaterialTheme.typography.bodySmall)
                            Text("बीएमआई: ${bmi.bmiValue} (${bmi.labelHindi})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = bmi.color)
                        }
                    }

                    item {
                        Text("ग्राउंड टाइमिंग एवं रिकॉर्ड", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("1600m दौड़:", style = MaterialTheme.typography.bodySmall)
                                Text(s.time1600m, fontWeight = FontWeight.Bold, color = SaffronPrimary, style = MaterialTheme.typography.bodySmall)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("5 KM टाइमिंग:", style = MaterialTheme.typography.bodySmall)
                                Text(s.time5km, fontWeight = FontWeight.Bold, color = OliveTertiary, style = MaterialTheme.typography.bodySmall)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("पुश-अप्स:", style = MaterialTheme.typography.bodySmall)
                                Text("${s.pushups} Reps", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("बीम / पुल-अप्स:", style = MaterialTheme.typography.bodySmall)
                                Text("${s.pullups} Beam", fontWeight = FontWeight.Bold, color = GoldAccent, style = MaterialTheme.typography.bodySmall)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("समग्र स्कोर:", style = MaterialTheme.typography.bodySmall)
                                Text("${s.overallScore}/100", fontWeight = FontWeight.Bold, color = OliveTertiary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedStudentForDetails = null },
                    colors = ButtonDefaults.buttonColors(containerColor = OliveTertiary)
                ) {
                    Text("बंद करें")
                }
            }
        )
    }

    if (showAddStudentDialog) {
        val nextId = remember(allStudents) {
            com.example.util.ProfileUtils.generateNextStudentId(allStudents)
        }

        var name by remember { mutableStateOf("") }
        var fatherName by remember { mutableStateOf("") }
        var village by remember { mutableStateOf("मौरिकला") }
        var dob by remember { mutableStateOf("2005-05-15") }
        var mobile by remember { mutableStateOf("") }
        var gender by remember { mutableStateOf("Male") }
        var education by remember { mutableStateOf("12th Pass") }
        var goal by remember { mutableStateOf("Indian Army") }
        var height by remember { mutableStateOf("172") }
        var weight by remember { mutableStateOf("63") }
        var time1600 by remember { mutableStateOf("5:30") }
        var pushups by remember { mutableStateOf("40") }
        var validationError by remember { mutableStateOf<String?>(null) }

        val calculatedAge = remember(dob) {
            val calc = com.example.util.ProfileUtils.calculateAgeFromDob(dob)
            if (calc > 0) calc else 20
        }

        val liveHeight = height.toDoubleOrNull() ?: 172.0
        val liveWeight = weight.toDoubleOrNull() ?: 63.0
        val liveBmi = remember(liveHeight, liveWeight) {
            com.example.util.ProfileUtils.getBmiCategory(liveHeight, liveWeight)
        }

        AlertDialog(
            onDismissRequest = { showAddStudentDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, tint = SaffronPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("नया अभ्यर्थी पंजीयन (Enroll Student)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (validationError != null) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = validationError ?: "",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }

                    // Generated Unique ID
                    item {
                        Surface(
                            color = SaffronContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("स्वतः जनरेटेड स्टूडेंट आईडी:", style = MaterialTheme.typography.bodySmall, color = OnSaffronContainer)
                                Text(nextId, fontWeight = FontWeight.ExtraBold, color = SaffronDark, style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }

                    item { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("पूरा नाम (Full Name) *") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(value = fatherName, onValueChange = { fatherName = it }, label = { Text("पिता का नाम (Father's Name)") }, modifier = Modifier.fillMaxWidth()) }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("मोबाइल नंबर") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone), modifier = Modifier.weight(1f))
                            OutlinedTextField(value = village, onValueChange = { village = it }, label = { Text("गाँव (Village)") }, modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = dob, onValueChange = { dob = it }, label = { Text("जन्म तिथि (YYYY-MM-DD)") }, modifier = Modifier.weight(1.3f))
                            OutlinedTextField(value = "$calculatedAge वर्ष", onValueChange = {}, readOnly = true, enabled = false, label = { Text("आयु (Auto)") }, modifier = Modifier.weight(0.9f))
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = gender, onValueChange = { gender = it }, label = { Text("लिंग (Gender)") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = education, onValueChange = { education = it }, label = { Text("शिक्षा (Education)") }, modifier = Modifier.weight(1f))
                        }
                    }
                    item { OutlinedTextField(value = goal, onValueChange = { goal = it }, label = { Text("भर्ती लक्ष्य (Army/Police/SSC)") }, modifier = Modifier.fillMaxWidth()) }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = height, onValueChange = { height = it }, label = { Text("ऊंचाई (cm)") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("वजन (kg)") }, modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        Surface(
                            color = liveBmi.color.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "बीएमआई: ${liveBmi.bmiValue} • ${liveBmi.labelHindi}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = liveBmi.color,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = time1600, onValueChange = { time1600 = it }, label = { Text("1600m लक्ष्य समय") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = pushups, onValueChange = { pushups = it }, label = { Text("पुश-अप्स") }, modifier = Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanMobile = mobile.trim().filter { it.isDigit() }
                        if (name.isBlank()) {
                            validationError = "कृपया छात्र का पूरा नाम दर्ज करें।"
                        } else if (cleanMobile.length != 10) {
                            validationError = "कृपया 10 अंकों का वैध मोबाइल नंबर दर्ज करें! (10 Digits Required)"
                        } else if (allStudents.any { it.mobileNumber.trim().filter { c -> c.isDigit() } == cleanMobile }) {
                            validationError = "यह मोबाइल नंबर ($cleanMobile) पहले से पंजीकृत है! कृपया दूसरा नंबर दर्ज करें।"
                        } else {
                            val newStudent = StudentProfile(
                                studentId = nextId,
                                fullName = name.trim(),
                                fatherName = fatherName.trim().ifEmpty { "श्री रामकुमार" },
                                village = village.trim().ifEmpty { "मौरिकला" },
                                age = calculatedAge,
                                dob = dob.trim().ifEmpty { "2005-05-15" },
                                gender = gender.trim().ifEmpty { "Male" },
                                education = education.trim().ifEmpty { "12th Pass" },
                                mobileNumber = cleanMobile,
                                recruitmentGoal = goal.trim().ifEmpty { "Indian Army" },
                                heightCm = height.toDoubleOrNull() ?: 172.0,
                                weightKg = weight.toDoubleOrNull() ?: 63.0,
                                chestNormalCm = 81.0,
                                chestExpandedCm = 86.0,
                                time1600m = time1600.trim().ifEmpty { "5:30" },
                                time400m = "1:08",
                                time800m = "2:25",
                                time5km = "21:30",
                                pushups = pushups.toIntOrNull() ?: 35,
                                situps = 45,
                                pullups = 8,
                                squats = 50,
                                plankSeconds = 90,
                                longJumpFeet = 15.0,
                                highJumpFeet = 4.0,
                                shotPutMeters = 7.2,
                                attendanceStreakDays = 1,
                                studyTargetPercentage = 0,
                                overallScore = 80
                            )
                            onAddStudent(newStudent)
                            showAddStudentDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                    modifier = Modifier.testTag("submit_new_student_button")
                ) {
                    Text("पंजीयन करें (Save)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStudentDialog = false }) { Text("रद्द करें") }
            }
        )
    }
}

@Composable
fun AdminActionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAttendanceBatchScreen(
    allStudents: List<StudentProfile>,
    onSaveBatchAttendance: (Map<String, String>, String) -> Unit = { _, _ -> },
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val todayDateStr = remember {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
    }
    var selectedDate by remember { mutableStateOf(todayDateStr) }

    val statusMap = remember {
        mutableStateMapOf<String, String>().apply {
            allStudents.forEach { s -> put(s.studentId, "Present") }
        }
    }
    var savedSuccess by remember { mutableStateOf(false) }

    val totalStudents = allStudents.size
    val presentCount = statusMap.values.count { it == "Present" }
    val absentCount = statusMap.values.count { it == "Absent" }
    val leaveCount = statusMap.values.count { it == "Leave" }
    val lateCount = statusMap.values.count { it == "Late" }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_attendance_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "दैनिक बैच उपस्थिति (Trainer Attendance)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ग्राउंड प्रशिक्षण उपस्थिति सत्यापन • दिनांक चुनें एवं उपस्थिति दर्ज करें",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Date selector field
                    OutlinedTextField(
                        value = selectedDate,
                        onValueChange = {
                            selectedDate = it
                            savedSuccess = false
                        },
                        label = { Text("उपस्थिति दिनांक (Date YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = SaffronPrimary)
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Summary statistics card
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "$totalStudents", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
                                Text(text = "कुल छात्र", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "$presentCount", fontWeight = FontWeight.ExtraBold, color = StatusPresent, style = MaterialTheme.typography.titleMedium)
                                Text(text = "उपस्थित", style = MaterialTheme.typography.labelSmall, color = StatusPresent)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "$absentCount", fontWeight = FontWeight.ExtraBold, color = StatusAbsent, style = MaterialTheme.typography.titleMedium)
                                Text(text = "अनुपस्थित", style = MaterialTheme.typography.labelSmall, color = StatusAbsent)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "$leaveCount", fontWeight = FontWeight.ExtraBold, color = StatusLeave, style = MaterialTheme.typography.titleMedium)
                                Text(text = "अवकाश", style = MaterialTheme.typography.labelSmall, color = StatusLeave)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                allStudents.forEach { statusMap[it.studentId] = "Present" }
                                savedSuccess = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OliveTertiary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("सभी उपस्थित", style = MaterialTheme.typography.labelSmall)
                        }

                        Button(
                            onClick = {
                                onSaveBatchAttendance(statusMap.toMap(), selectedDate)
                                savedSuccess = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("उपस्थिति सेव करें", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    if (savedSuccess) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "✓ दिनांक $selectedDate की उपस्थिति सफलतापूर्वक दर्ज की गई!",
                            color = OliveTertiary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        items(allStudents) { student ->
            val currentStatus = statusMap[student.studentId] ?: "Present"

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = student.fullName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(text = "${student.studentId} • ${student.village}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        StatusBadge(status = currentStatus)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Present", "Absent", "Leave", "Late").forEach { st ->
                            val isSel = currentStatus == st
                            val stColor = when (st) {
                                "Present" -> OliveTertiary
                                "Late" -> StatusLate
                                "Leave" -> StatusLeave
                                else -> StatusAbsent
                            }
                            val stLabel = when (st) {
                                "Present" -> "उपस्थित"
                                "Absent" -> "अनुपस्थित"
                                "Leave" -> "अवकाश"
                                else -> "देरी"
                            }

                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    statusMap[student.studentId] = st
                                    savedSuccess = false
                                },
                                label = { Text(stLabel, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = stColor.copy(alpha = 0.2f),
                                    selectedLabelColor = stColor
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminWorkoutPlanScreen(
    currentPlan: DailyWorkoutPlan?,
    onSavePlan: (DailyWorkoutPlan) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf(currentPlan?.title ?: "ग्राउंड रनिंग एवं बीम स्पेशल ट्रेनिंग") }
    var instructions by remember { mutableStateOf(currentPlan?.instructions ?: "सुबह 5:30 बजे 3 KM वार्म-अप रनिंग, 1600m ट्रायल, 50 पुश-अप्स व 10 बीम अभ्यास।") }
    var runKm by remember { mutableStateOf(currentPlan?.targetRunningKm?.toString() ?: "3.0") }
    var pushups by remember { mutableStateOf(currentPlan?.targetPushups?.toString() ?: "50") }
    var situps by remember { mutableStateOf(currentPlan?.targetSitups?.toString() ?: "50") }
    var pullups by remember { mutableStateOf(currentPlan?.targetPullups?.toString() ?: "10") }
    var isSaved by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_workout_plan_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "दैनिक ट्रेनिंग प्लान बनाएं (Create Workout Plan)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "यह प्लान सभी छात्रों के होम एवं वर्कआउट स्क्रीन पर प्रदर्शित होगा।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("प्लान का शीर्षक") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = instructions,
                        onValueChange = { instructions = it },
                        label = { Text("प्रशिक्षक निर्देश एवं तकनीक") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = runKm,
                            onValueChange = { runKm = it },
                            label = { Text("रनिंग (KM)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = pushups,
                            onValueChange = { pushups = it },
                            label = { Text("पुश-अप्स") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = situps,
                            onValueChange = { situps = it },
                            label = { Text("सिट-अप्स") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = pullups,
                            onValueChange = { pullups = it },
                            label = { Text("पुल-अप्स (बीम)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = {
                            val plan = DailyWorkoutPlan(
                                date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                                title = title,
                                targetRunningKm = runKm.toDoubleOrNull() ?: 3.0,
                                targetPushups = pushups.toIntOrNull() ?: 50,
                                targetSitups = situps.toIntOrNull() ?: 50,
                                targetPullups = pullups.toIntOrNull() ?: 10,
                                instructions = instructions
                            )
                            onSavePlan(plan)
                            isSaved = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Publish, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ट्रेनिंग प्लान प्रकाशित करें (Publish)")
                    }

                    if (isSaved) {
                        Text(
                            text = "✓ नया ट्रेनिंग प्लान सफलतापूर्वक प्रकाशित किया गया!",
                            color = OliveTertiary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminNoticeAndRecruitmentScreen(
    notices: List<Notice>,
    recruitmentList: List<RecruitmentInfo>,
    onPublishNotice: (title: String, content: String, category: String, isUrgent: Boolean) -> Unit,
    onDeleteNotice: (Notice) -> Unit,
    onAddRecruitment: (RecruitmentInfo) -> Unit,
    onDeleteRecruitment: (RecruitmentInfo) -> Unit,
    modifier: Modifier = Modifier,
    onUpdateNotice: ((Notice) -> Unit)? = null,
    onTogglePinNotice: ((Notice) -> Unit)? = null,
    onUpdateRecruitment: ((RecruitmentInfo) -> Unit)? = null
) {
    var showNoticeDialog by remember { mutableStateOf(false) }
    var showJobDialog by remember { mutableStateOf(false) }
    var editingNotice by remember { mutableStateOf<Notice?>(null) }
    var editingRecruitment by remember { mutableStateOf<RecruitmentInfo?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_notices_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "सूचना व भर्ती प्रबंधन (Notices & Recruitment Control)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "अखाड़ा नोटिस प्रकाशित/संपादित करें एवं सरकारी भर्ती अलर्ट मैनेज करें।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showNoticeDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("नया नोटिस", style = MaterialTheme.typography.labelSmall)
                        }

                        Button(
                            onClick = { showJobDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = OliveTertiary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("नई भर्ती जोड़ें", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "सक्रिय अखाड़ा नोटिस सूची (${notices.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        items(notices) { notice ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (notice.isPinned) SaffronContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                ),
                border = CardDefaults.outlinedCardBorder().let {
                    if (notice.isPinned) androidx.compose.foundation.BorderStroke(1.5.dp, SaffronPrimary)
                    else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (notice.isPinned) {
                                Surface(color = SaffronDark, shape = RoundedCornerShape(4.dp)) {
                                    Text("📌 PIN", color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                }
                            }
                            if (notice.isUrgent) {
                                Surface(color = StatusAbsent, shape = RoundedCornerShape(4.dp)) {
                                    Text("URGENT", color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                }
                            }
                            Text(text = notice.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(text = "${notice.category} • ${notice.date} • ${notice.author}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Row {
                        IconButton(onClick = { editingNotice = notice }) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = SaffronPrimary)
                        }
                        IconButton(onClick = { onDeleteNotice(notice) }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = StatusAbsent)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "सरकारी भर्ती नोटिफिकेशन सूची (${recruitmentList.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        items(recruitmentList) { rec ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = rec.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(text = "पद: ${rec.postName} (${rec.totalPosts}) • अंतिम तिथि: ${rec.lastDate}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Row {
                        IconButton(onClick = { editingRecruitment = rec }) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = OliveTertiary)
                        }
                        IconButton(onClick = { onDeleteRecruitment(rec) }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = StatusAbsent)
                        }
                    }
                }
            }
        }
    }

    // Add Notice Dialog
    if (showNoticeDialog) {
        var nTitle by remember { mutableStateOf("") }
        var nContent by remember { mutableStateOf("") }
        var nCategory by remember { mutableStateOf("प्रशिक्षण") }
        var isUrgent by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showNoticeDialog = false },
            title = { Text("नया नोटिस जारी करें", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = nTitle, onValueChange = { nTitle = it }, label = { Text("शीर्षक (Title)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = nContent, onValueChange = { nContent = it }, label = { Text("विवरण (Details)") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                    OutlinedTextField(value = nCategory, onValueChange = { nCategory = it }, label = { Text("श्रेणी (प्रशिक्षण / परीक्षा / भर्ती / सामान्य)") }, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isUrgent, onCheckedChange = { isUrgent = it })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("अति महत्वपूर्ण / जरूरी सूचना (Urgent Alert)")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nTitle.isNotEmpty() && nContent.isNotEmpty()) {
                            onPublishNotice(nTitle, nContent, nCategory, isUrgent)
                            showNoticeDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                ) {
                    Text("प्रकाशित करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoticeDialog = false }) { Text("रद्द करें") }
            }
        )
    }

    // Edit Notice Dialog
    editingNotice?.let { noticeToEdit ->
        var nTitle by remember { mutableStateOf(noticeToEdit.title) }
        var nContent by remember { mutableStateOf(noticeToEdit.content) }
        var nCategory by remember { mutableStateOf(noticeToEdit.category) }
        var nAuthor by remember { mutableStateOf(noticeToEdit.author) }
        var isUrgent by remember { mutableStateOf(noticeToEdit.isUrgent) }
        var isPinned by remember { mutableStateOf(noticeToEdit.isPinned) }

        AlertDialog(
            onDismissRequest = { editingNotice = null },
            title = { Text("नोटिस संपादित करें (Edit Notice)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = nTitle, onValueChange = { nTitle = it }, label = { Text("शीर्षक (Title)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = nContent, onValueChange = { nContent = it }, label = { Text("विवरण (Details)") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                    OutlinedTextField(value = nCategory, onValueChange = { nCategory = it }, label = { Text("श्रेणी (Category)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = nAuthor, onValueChange = { nAuthor = it }, label = { Text("जारीकर्ता (Author)") }, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isUrgent, onCheckedChange = { isUrgent = it })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("जरूरी सूचना (Urgent)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isPinned, onCheckedChange = { isPinned = it })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("शीर्ष पर पिन करें (Pin Notice)")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = noticeToEdit.copy(
                            title = nTitle,
                            content = nContent,
                            category = nCategory,
                            author = nAuthor,
                            isUrgent = isUrgent,
                            priority = if (isUrgent) "URGENT" else "NORMAL",
                            isPinned = isPinned
                        )
                        onUpdateNotice?.invoke(updated)
                        editingNotice = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                ) {
                    Text("अपडेट करें (Save)")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingNotice = null }) { Text("रद्द करें") }
            }
        )
    }

    // Add Recruitment Dialog
    if (showJobDialog) {
        var jTitle by remember { mutableStateOf("") }
        var jPost by remember { mutableStateOf("") }
        var jCount by remember { mutableStateOf("") }
        var jElig by remember { mutableStateOf("10th / 12th Pass") }
        var jAge by remember { mutableStateOf("18-23 वर्ष") }
        var jDate by remember { mutableStateOf("2026-09-30") }
        var jUrl by remember { mutableStateOf("https://joinindianarmy.nic.in") }

        AlertDialog(
            onDismissRequest = { showJobDialog = false },
            title = { Text("नई सरकारी भर्ती जोड़ें", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { OutlinedTextField(value = jTitle, onValueChange = { jTitle = it }, label = { Text("भर्ती शीर्षक") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(value = jPost, onValueChange = { jPost = it }, label = { Text("पद का नाम") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(value = jCount, onValueChange = { jCount = it }, label = { Text("कुल पद (Total Posts)") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(value = jElig, onValueChange = { jElig = it }, label = { Text("योग्यता") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(value = jAge, onValueChange = { jAge = it }, label = { Text("आयु सीमा") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(value = jDate, onValueChange = { jDate = it }, label = { Text("अंतिम तिथि") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(value = jUrl, onValueChange = { jUrl = it }, label = { Text("ऑफिशियल वेबसाइट लिंक") }, modifier = Modifier.fillMaxWidth()) }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (jTitle.isNotEmpty() && jPost.isNotEmpty()) {
                            val newRec = RecruitmentInfo(
                                recruitmentName = "$jTitle - $jPost",
                                organization = jCount.ifEmpty { "1000+ Posts" },
                                eligibility = jElig,
                                ageLimit = jAge,
                                heightRequirement = "168 cm",
                                chestRequirement = "81-86 cm",
                                physicalTest = "1600m Running, Long Jump, High Jump",
                                writtenExam = "Online CBT Exam",
                                syllabus = "Maths, Reasoning, GK, Language",
                                importantDocuments = "Aadhaar, 10th/12th Marksheet, Domicile",
                                importantDates = jDate,
                                officialWebsiteLink = jUrl
                            )
                            onAddRecruitment(newRec)
                            showJobDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                ) {
                    Text("जोड़ें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJobDialog = false }) { Text("रद्द करें") }
            }
        )
    }

    // Edit Recruitment Dialog (Phase 2C Admin Enhancement)
    editingRecruitment?.let { recToEdit ->
        var jTitle by remember { mutableStateOf(recToEdit.recruitmentName) }
        var jOrg by remember { mutableStateOf(recToEdit.organization) }
        var jElig by remember { mutableStateOf(recToEdit.eligibility) }
        var jAge by remember { mutableStateOf(recToEdit.ageLimit) }
        var jDates by remember { mutableStateOf(recToEdit.importantDates) }
        var jHeight by remember { mutableStateOf(recToEdit.heightRequirement) }
        var jChest by remember { mutableStateOf(recToEdit.chestRequirement) }
        var jPhysical by remember { mutableStateOf(recToEdit.physicalTest) }
        var jUrl by remember { mutableStateOf(recToEdit.officialWebsiteLink) }

        AlertDialog(
            onDismissRequest = { editingRecruitment = null },
            title = { Text("भर्ती अधिसूचना संपादित करें (Edit Recruitment)", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { OutlinedTextField(value = jTitle, onValueChange = { jTitle = it }, label = { Text("भर्ती नाम / पद") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(value = jOrg, onValueChange = { jOrg = it }, label = { Text("विभाग / संगठन") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(value = jElig, onValueChange = { jElig = it }, label = { Text("शैक्षणिक योग्यता") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(value = jAge, onValueChange = { jAge = it }, label = { Text("आयु सीमा") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(value = jDates, onValueChange = { jDates = it }, label = { Text("आवेदन व परीक्षा तिथियां") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(value = jHeight, onValueChange = { jHeight = it }, label = { Text("ऊंचाई मानक") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(value = jChest, onValueChange = { jChest = it }, label = { Text("सीना मानक") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(value = jPhysical, onValueChange = { jPhysical = it }, label = { Text("शारीरिक दक्षता (PET)") }, modifier = Modifier.fillMaxWidth(), minLines = 2) }
                    item { OutlinedTextField(value = jUrl, onValueChange = { jUrl = it }, label = { Text("ऑफिशियल वेबसाइट URL") }, modifier = Modifier.fillMaxWidth()) }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = recToEdit.copy(
                            recruitmentName = jTitle,
                            organization = jOrg,
                            eligibility = jElig,
                            ageLimit = jAge,
                            importantDates = jDates,
                            heightRequirement = jHeight,
                            chestRequirement = jChest,
                            physicalTest = jPhysical,
                            officialWebsiteLink = jUrl
                        )
                        onUpdateRecruitment?.invoke(updated)
                        editingRecruitment = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OliveTertiary)
                ) {
                    Text("अपडेट करें (Save)")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingRecruitment = null }) { Text("रद्द करें") }
            }
        )
    }
}
