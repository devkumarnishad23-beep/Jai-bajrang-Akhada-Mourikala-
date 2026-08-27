package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiCoachEngine
import com.example.data.ai.AiCoachInsight
import com.example.data.ai.PerformanceScoreBreakdown
import com.example.data.db.AppDatabase
import com.example.data.model.*
import com.example.data.recruitment.*
import com.example.data.repository.AppRepository
import com.example.util.AdminSecurityManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository
    private val database: AppDatabase

    init {
        database = AppDatabase.getDatabase(application, viewModelScope)
        repository = AppRepository(database.appDao())
        viewModelScope.launch {
            repository.ensureDataSeeded()
        }
    }

    private val todayDateStr: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // Admin PIN Security State
    private val _isAdminPinConfigured = MutableStateFlow(AdminSecurityManager.isPinConfigured(getApplication()))
    val isAdminPinConfigured: StateFlow<Boolean> = _isAdminPinConfigured.asStateFlow()

    fun refreshAdminPinStatus() {
        _isAdminPinConfigured.value = AdminSecurityManager.isPinConfigured(getApplication())
    }

    // Role state
    private val _currentRole = MutableStateFlow("STUDENT") // "STUDENT" or "ADMIN"
    val currentRole: StateFlow<String> = _currentRole.asStateFlow()

    // Login state
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // Selected Active Student ID
    private val _activeStudentId = MutableStateFlow("JBA-2026-001")
    val activeStudentId: StateFlow<String> = _activeStudentId.asStateFlow()

    fun loginAsStudent(studentIdInput: String): Boolean {
        val trimmed = studentIdInput.trim()
        val match = allStudents.value.find {
            it.studentId.equals(trimmed, ignoreCase = true) ||
            it.mobileNumber.equals(trimmed, ignoreCase = true)
        }
        if (match != null) {
            _activeStudentId.value = match.studentId
            _currentRole.value = "STUDENT"
            _isLoggedIn.value = true
            return true
        }
        return false
    }

    /**
     * Authenticates Admin using securely stored salted hash.
     * Rejects empty/default bypasses.
     */
    fun loginAsAdmin(pinInput: String): Boolean {
        val trimmed = pinInput.trim()
        val isConfigured = AdminSecurityManager.isPinConfigured(getApplication())
        if (!isConfigured) {
            return false
        }
        val isValid = AdminSecurityManager.verifyAdminPin(getApplication(), trimmed)
        if (isValid) {
            _currentRole.value = "ADMIN"
            _isLoggedIn.value = true
            return true
        }
        return false
    }

    /**
     * Initial one-time Admin PIN configuration.
     */
    fun setupInitialAdminPin(pin: String): Result<Unit> {
        val result = AdminSecurityManager.setupInitialPin(getApplication(), pin)
        if (result.isSuccess) {
            _isAdminPinConfigured.value = true
            _currentRole.value = "ADMIN"
            _isLoggedIn.value = true
        }
        return result
    }

    /**
     * Changes existing Admin PIN securely.
     */
    fun changeAdminPin(currentPin: String, newPin: String, confirmPin: String): Result<Unit> {
        val result = AdminSecurityManager.changeAdminPin(getApplication(), currentPin, newPin, confirmPin)
        if (result.isSuccess) {
            _isAdminPinConfigured.value = true
        }
        return result
    }

    /**
     * Registers a new student securely after validating mobile number uniqueness.
     */
    suspend fun registerStudentSecurely(student: StudentProfile): Result<StudentProfile> {
        val trimmedMobile = student.mobileNumber.trim()
        val digitsOnly = trimmedMobile.filter { it.isDigit() }
        if (digitsOnly.length != 10) {
            return Result.failure(IllegalArgumentException("कृपया 10 अंकों का वैध मोबाइल नंबर दर्ज करें! (10 digits required)"))
        }

        val isAlreadyRegistered = repository.isMobileRegistered(trimmedMobile)
        if (isAlreadyRegistered) {
            return Result.failure(IllegalArgumentException("यह मोबाइल नंबर ($trimmedMobile) पहले से पंजीकृत है! कृपया दूसरा नंबर दर्ज करें या अपनी आईडी से लॉगिन करें।"))
        }

        val studentToSave = student.copy(mobileNumber = trimmedMobile)
        repository.insertStudent(studentToSave)
        _activeStudentId.value = studentToSave.studentId
        _currentRole.value = "STUDENT"
        _isLoggedIn.value = true
        return Result.success(studentToSave)
    }

    fun loginAsDemoStudent(studentId: String) {
        _activeStudentId.value = studentId
        _currentRole.value = "STUDENT"
        _isLoggedIn.value = true
    }

    fun loginAsDemoAdmin() {
        _currentRole.value = "ADMIN"
        _isLoggedIn.value = true
    }

    fun logout() {
        _isLoggedIn.value = false
    }

    // Active Subject filter for study
    private val _selectedSubject = MutableStateFlow("Mathematics")
    val selectedSubject: StateFlow<String> = _selectedSubject.asStateFlow()

    // Flows from repository
    val allStudents: StateFlow<List<StudentProfile>> = repository.allStudents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeStudent: StateFlow<StudentProfile?> = _activeStudentId
        .flatMapLatest { id -> repository.getStudent(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeAttendance: StateFlow<List<AttendanceRecord>> = _activeStudentId
        .flatMapLatest { id -> repository.getAttendanceForStudent(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAttendanceRecords: StateFlow<List<AttendanceRecord>> = repository.allAttendance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayAttendance: StateFlow<AttendanceRecord?> = _activeStudentId
        .flatMapLatest { id -> repository.getTodayAttendance(id, todayDateStr) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeTrainingRecords: StateFlow<List<TrainingRecord>> = _activeStudentId
        .flatMapLatest { id -> repository.getTrainingRecordsForStudent(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayTraining: StateFlow<TrainingRecord?> = _activeStudentId
        .flatMapLatest { id -> repository.getTodayTraining(id, todayDateStr) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allTrainingRecords: StateFlow<List<TrainingRecord>> = repository.allTrainingRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeWorkouts: StateFlow<List<WorkoutRecord>> = _activeStudentId
        .flatMapLatest { id -> repository.getWorkoutsForStudent(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayWorkout: StateFlow<WorkoutRecord?> = _activeStudentId
        .flatMapLatest { id -> repository.getTodayWorkout(id, todayDateStr) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val latestWorkoutPlan: StateFlow<DailyWorkoutPlan?> = repository.latestWorkoutPlan
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allChapters: StateFlow<List<Chapter>> = repository.allChapters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayStudyTargets: StateFlow<List<Chapter>> = repository.todayStudyTargets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Phase 2B-1 Study & Question Bank Flows
    val allSubjects: StateFlow<List<StudySubject>> = repository.allSubjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSubjects: StateFlow<List<StudySubject>> = repository.activeSubjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTopics: StateFlow<List<StudyTopic>> = repository.allTopics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allQuestions: StateFlow<List<Question>> = repository.allQuestions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeQuestions: StateFlow<List<Question>> = repository.activeQuestions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStudyAttempts: StateFlow<List<StudyAttempt>> = repository.allStudyAttempts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studentStudyAttempts: StateFlow<List<StudyAttempt>> = _activeStudentId
        .flatMapLatest { id -> repository.getStudyAttemptsForStudent(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMockTests: StateFlow<List<MockTest>> = repository.allMockTests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTestAttempts: StateFlow<List<TestAttempt>> = repository.allTestAttempts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studentTestAttempts: StateFlow<List<TestAttempt>> = _activeStudentId
        .flatMapLatest { id -> repository.getAttemptsForStudent(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotices: StateFlow<List<Notice>> = repository.allNotices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRecruitmentInfo: StateFlow<List<RecruitmentInfo>> = repository.allRecruitmentInfo
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Akhada Content Management Flows ---
    val allTrainers: StateFlow<List<Trainer>> = repository.allTrainers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGalleryItems: StateFlow<List<GalleryItem>> = repository.allGalleryItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSuccessStories: StateFlow<List<SuccessStory>> = repository.allSuccessStories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contactInfo: StateFlow<ContactInfo?> = repository.contactInfo
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun addTrainer(trainer: Trainer) {
        viewModelScope.launch { repository.addTrainer(trainer) }
    }

    fun updateTrainer(trainer: Trainer) {
        viewModelScope.launch { repository.updateTrainer(trainer) }
    }

    fun deleteTrainer(trainer: Trainer) {
        viewModelScope.launch { repository.deleteTrainer(trainer) }
    }

    fun addGalleryItem(item: GalleryItem) {
        viewModelScope.launch { repository.addGalleryItem(item) }
    }

    fun deleteGalleryItem(item: GalleryItem) {
        viewModelScope.launch { repository.deleteGalleryItem(item) }
    }

    fun addSuccessStory(story: SuccessStory) {
        viewModelScope.launch { repository.addSuccessStory(story) }
    }

    fun updateSuccessStory(story: SuccessStory) {
        viewModelScope.launch { repository.updateSuccessStory(story) }
    }

    fun deleteSuccessStory(story: SuccessStory) {
        viewModelScope.launch { repository.deleteSuccessStory(story) }
    }

    fun updateContactInfo(info: ContactInfo) {
        viewModelScope.launch { repository.updateContactInfo(info) }
    }

    // Overall Performance & AI Coach Insights
    val unifiedPerformanceState: StateFlow<com.example.data.analytics.UnifiedPerformanceState> = combine(
        listOf(
            activeStudent,
            activeSubjects,
            allTopics,
            activeQuestions,
            studentStudyAttempts,
            studentTestAttempts,
            activeAttendance,
            activeWorkouts,
            activeTrainingRecords
        )
    ) { array ->
        val student = array[0] as? StudentProfile
        @Suppress("UNCHECKED_CAST")
        val subjects = array[1] as List<StudySubject>
        @Suppress("UNCHECKED_CAST")
        val topics = array[2] as List<StudyTopic>
        @Suppress("UNCHECKED_CAST")
        val questions = array[3] as List<Question>
        @Suppress("UNCHECKED_CAST")
        val studyAtt = array[4] as List<StudyAttempt>
        @Suppress("UNCHECKED_CAST")
        val testAtt = array[5] as List<TestAttempt>
        @Suppress("UNCHECKED_CAST")
        val attendance = array[6] as List<AttendanceRecord>
        @Suppress("UNCHECKED_CAST")
        val workouts = array[7] as List<WorkoutRecord>
        @Suppress("UNCHECKED_CAST")
        val training = array[8] as List<TrainingRecord>

        com.example.data.analytics.PerformanceInsightEngine.buildUnifiedPerformanceState(
            student = student,
            subjects = subjects,
            topics = topics,
            questions = questions,
            studyAttempts = studyAtt,
            testAttempts = testAtt,
            attendanceRecords = attendance,
            workoutRecords = workouts,
            trainingRecords = training,
            currentDate = todayDateStr
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        com.example.data.analytics.PerformanceInsightEngine.buildUnifiedPerformanceState(
            student = null,
            subjects = emptyList(),
            topics = emptyList(),
            questions = emptyList(),
            studyAttempts = emptyList(),
            testAttempts = emptyList(),
            attendanceRecords = emptyList(),
            workoutRecords = emptyList(),
            trainingRecords = emptyList(),
            currentDate = todayDateStr
        )
    )

    val performanceScore: StateFlow<PerformanceScoreBreakdown> = combine(
        activeAttendance,
        activeWorkouts,
        allChapters,
        studentTestAttempts
    ) { att, wkt, chp, attm ->
        AiCoachEngine.calculateOverallPerformance(att, wkt, chp, attm)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        PerformanceScoreBreakdown(84, 18.0, 26.0, 16.0, 24.0, "A (बहुत अच्छा)", "उत्कृष्ट संतुलन")
    )

    val aiCoachInsights: StateFlow<List<AiCoachInsight>> = combine(
        listOf(
            activeStudent,
            activeAttendance,
            activeWorkouts,
            allChapters,
            studentTestAttempts,
            studentStudyAttempts,
            unifiedPerformanceState
        )
    ) { array ->
        val student = array[0] as? StudentProfile
        @Suppress("UNCHECKED_CAST")
        val att = array[1] as List<AttendanceRecord>
        @Suppress("UNCHECKED_CAST")
        val wkt = array[2] as List<WorkoutRecord>
        @Suppress("UNCHECKED_CAST")
        val chp = array[3] as List<Chapter>
        @Suppress("UNCHECKED_CAST")
        val testAttm = array[4] as List<TestAttempt>
        @Suppress("UNCHECKED_CAST")
        val studyAttm = array[5] as List<StudyAttempt>
        val uState = array[6] as? com.example.data.analytics.UnifiedPerformanceState

        if (student != null) {
            AiCoachEngine.generatePersonalizedInsights(
                student = student,
                attendanceRecords = att,
                workoutRecords = wkt,
                chapters = chp,
                testAttempts = testAttm,
                studyAttempts = studyAttm,
                unifiedState = uState
            )
        } else emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Phase 2C: Recruitment & Eligibility Intelligence State ---
    private val _selectedRecruitmentCategory = MutableStateFlow("ARMY_AGNIVEER_GD")
    val selectedRecruitmentCategory: StateFlow<String> = _selectedRecruitmentCategory.asStateFlow()

    val selectedRecruitmentProfile: StateFlow<RecruitmentCategoryProfile> = _selectedRecruitmentCategory
        .map { catId -> RecruitmentStandardRepository.findProfileByIdOrGoal(catId) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            RecruitmentStandardRepository.allCategoryProfiles.first()
        )

    val studentEligibilityReport: StateFlow<StudentEligibilityReport> = combine(
        activeStudent,
        selectedRecruitmentProfile
    ) { student, profile ->
        EligibilityIntelligenceEngine.evaluateEligibility(student, profile)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        EligibilityIntelligenceEngine.evaluateEligibility(null, RecruitmentStandardRepository.allCategoryProfiles.first())
    )

    val physicalStandardComparison: StateFlow<List<PhysicalStandardComparisonItem>> = combine(
        activeStudent,
        selectedRecruitmentProfile
    ) { student, profile ->
        EligibilityIntelligenceEngine.comparePhysicalStandards(student, profile)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val personalizedRecruitmentRecommendations: StateFlow<List<String>> = combine(
        listOf(
            activeStudent,
            selectedRecruitmentProfile,
            activeWorkouts,
            studentTestAttempts,
            studentStudyAttempts
        )
    ) { array ->
        val student = array[0] as? StudentProfile
        val profile = array[1] as? RecruitmentCategoryProfile ?: RecruitmentStandardRepository.allCategoryProfiles.first()
        @Suppress("UNCHECKED_CAST")
        val wkt = array[2] as List<WorkoutRecord>
        @Suppress("UNCHECKED_CAST")
        val testAttm = array[3] as List<TestAttempt>
        @Suppress("UNCHECKED_CAST")
        val studyAttm = array[4] as List<StudyAttempt>

        EligibilityIntelligenceEngine.generatePersonalizedRecruitmentRecommendations(
            student = student,
            targetProfile = profile,
            workoutRecords = wkt,
            testAttempts = testAttm,
            studyAttempts = studyAttm
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Phase 2C: Notice Intelligence State ---
    private val _selectedNoticeCategory = MutableStateFlow("ALL")
    val selectedNoticeCategory: StateFlow<String> = _selectedNoticeCategory.asStateFlow()

    private val _noticeSearchQuery = MutableStateFlow("")
    val noticeSearchQuery: StateFlow<String> = _noticeSearchQuery.asStateFlow()

    val filteredSortedNotices: StateFlow<List<Notice>> = combine(
        allNotices,
        _selectedNoticeCategory,
        _noticeSearchQuery
    ) { notices, cat, query ->
        val filtered = EligibilityIntelligenceEngine.filterAndSortNotices(
            notices = notices,
            selectedCategory = cat,
            currentDate = todayDateStr
        )
        if (query.isBlank()) {
            filtered
        } else {
            val q = query.trim().lowercase()
            filtered.filter {
                it.title.lowercase().contains(q) ||
                it.content.lowercase().contains(q) ||
                it.category.lowercase().contains(q) ||
                it.author.lowercase().contains(q)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Active Quiz & Mock Test State ---
    private val _testTitle = MutableStateFlow("Daily Practice Quiz")
    val testTitle: StateFlow<String> = _testTitle.asStateFlow()

    private val _targetExam = MutableStateFlow("General Competition")
    val targetExam: StateFlow<String> = _targetExam.asStateFlow()

    private val _currentQuestions = MutableStateFlow<List<Question>>(emptyList())
    val currentQuestions: StateFlow<List<Question>> = _currentQuestions.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _userAnswers = MutableStateFlow<Map<Int, Int>>(emptyMap()) // index -> selectedOption (0..3)
    val userAnswers: StateFlow<Map<Int, Int>> = _userAnswers.asStateFlow()

    private val _markedForReview = MutableStateFlow<Set<Int>>(emptySet())
    val markedForReview: StateFlow<Set<Int>> = _markedForReview.asStateFlow()

    private val _timeRemainingSeconds = MutableStateFlow(1200)
    val timeRemainingSeconds: StateFlow<Int> = _timeRemainingSeconds.asStateFlow()

    private val _isTestCompleted = MutableStateFlow(false)
    val isTestCompleted: StateFlow<Boolean> = _isTestCompleted.asStateFlow()

    private val _latestAttemptResult = MutableStateFlow<TestAttempt?>(null)
    val latestAttemptResult: StateFlow<TestAttempt?> = _latestAttemptResult.asStateFlow()

    private var timerJob: Job? = null
    private var testTotalDurationSeconds = 1200
    private var activeTestId: Long = 0

    fun switchRole(role: String) {
        _currentRole.value = role
    }

    fun switchActiveStudent(studentId: String) {
        _activeStudentId.value = studentId
    }

    fun setSelectedSubject(subject: String) {
        _selectedSubject.value = subject
    }

    fun markTodayAttendance(status: String, remarks: String = "") {
        viewModelScope.launch {
            val record = AttendanceRecord(
                studentId = _activeStudentId.value,
                date = todayDateStr,
                status = status,
                remarks = remarks
            )
            repository.markAttendance(record)
        }
    }

    fun submitDailyWorkout(
        runningKm: Double,
        time1600Seconds: Int,
        pushups: Int,
        situps: Int,
        pullups: Int,
        squats: Int,
        notes: String
    ) {
        viewModelScope.launch {
            val prevWorkouts = activeWorkouts.value
            val prev1600 = prevWorkouts.firstOrNull()?.time1600mSeconds ?: (time1600Seconds + 20)
            val record = WorkoutRecord(
                studentId = _activeStudentId.value,
                date = todayDateStr,
                runningDistanceKm = runningKm,
                runningTargetKm = 3.0,
                time1600mSeconds = time1600Seconds,
                previous1600mSeconds = prev1600,
                pushupsDone = pushups,
                pushupsTarget = 50,
                situpsDone = situps,
                situpsTarget = 50,
                pullupsDone = pullups,
                pullupsTarget = 10,
                squatsDone = squats,
                squatsTarget = 50,
                plankSecondsDone = 90,
                notes = notes
            )
            repository.recordWorkout(record)
        }
    }

    fun submitTrainingRecord(
        studentId: String = _activeStudentId.value,
        date: String = todayDateStr,
        runningDistanceKm: Double,
        runningDuration: String,
        runningType: String,
        pushups: Int,
        situps: Int,
        pullups: Int,
        squats: Int,
        plankSeconds: Int,
        stretchingDone: Boolean = true,
        otherTraining: String = "",
        trainerNotes: String = ""
    ) {
        viewModelScope.launch {
            val record = TrainingRecord(
                studentId = studentId,
                date = date,
                runningDistanceKm = runningDistanceKm,
                runningDuration = runningDuration,
                runningType = runningType,
                pushups = pushups,
                situps = situps,
                pullups = pullups,
                squats = squats,
                plankSeconds = plankSeconds,
                stretchingDone = stretchingDone,
                otherTraining = otherTraining,
                trainerNotes = trainerNotes,
                timestamp = System.currentTimeMillis()
            )
            repository.recordTraining(record)
        }
    }

    fun saveTrainingRecord(record: TrainingRecord) {
        viewModelScope.launch {
            repository.recordTraining(record)
        }
    }

    fun updateTrainingRecord(record: TrainingRecord) {
        viewModelScope.launch {
            repository.updateTraining(record)
        }
    }

    fun deleteTrainingRecord(record: TrainingRecord) {
        viewModelScope.launch {
            repository.deleteTraining(record)
        }
    }

    fun getAttendanceForDate(date: String): Flow<List<AttendanceRecord>> = repository.getAttendanceByDate(date)

    fun toggleChapterCompletion(chapter: Chapter) {
        viewModelScope.launch {
            repository.toggleChapterCompletion(chapter)
        }
    }

    // --- Quiz & Mock Test Setup ---
    fun startChapterQuiz(chapter: Chapter, questionCount: Int = 10) {
        viewModelScope.launch {
            val questions = allQuestions.value.filter { it.chapterId == chapter.id }
                .ifEmpty { allQuestions.value.filter { it.subjectName == chapter.subjectName } }
                .ifEmpty { allQuestions.value }
                .take(questionCount)

            _testTitle.value = "${chapter.chapterName} - Quiz"
            _targetExam.value = chapter.subjectName
            _currentQuestions.value = questions
            _currentQuestionIndex.value = 0
            _userAnswers.value = emptyMap()
            _markedForReview.value = emptySet()
            _isTestCompleted.value = false
            _latestAttemptResult.value = null
            activeTestId = chapter.id
            testTotalDurationSeconds = questionCount * 60
            _timeRemainingSeconds.value = testTotalDurationSeconds

            startTimer()
        }
    }

    fun startMockTest(mockTest: MockTest) {
        viewModelScope.launch {
            val questions = allQuestions.value.shuffled().take(mockTest.totalQuestions)
                .ifEmpty { allQuestions.value }

            _testTitle.value = mockTest.title
            _targetExam.value = mockTest.targetExam
            _currentQuestions.value = questions
            _currentQuestionIndex.value = 0
            _userAnswers.value = emptyMap()
            _markedForReview.value = emptySet()
            _isTestCompleted.value = false
            _latestAttemptResult.value = null
            activeTestId = mockTest.id
            testTotalDurationSeconds = mockTest.durationMinutes * 60
            _timeRemainingSeconds.value = testTotalDurationSeconds

            startTimer()
        }
    }

    fun startCustomQuiz(subject: String, count: Int) {
        viewModelScope.launch {
            val questions = allQuestions.value
                .filter { if (subject == "All") true else it.subjectName == subject }
                .shuffled()
                .take(count)
                .ifEmpty { allQuestions.value }

            _testTitle.value = "$subject Speed Practice ($count Qs)"
            _targetExam.value = subject
            _currentQuestions.value = questions
            _currentQuestionIndex.value = 0
            _userAnswers.value = emptyMap()
            _markedForReview.value = emptySet()
            _isTestCompleted.value = false
            _latestAttemptResult.value = null
            activeTestId = 99
            testTotalDurationSeconds = count * 60
            _timeRemainingSeconds.value = testTotalDurationSeconds

            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timeRemainingSeconds.value > 0 && !_isTestCompleted.value) {
                delay(1000)
                _timeRemainingSeconds.value -= 1
            }
            if (_timeRemainingSeconds.value <= 0 && !_isTestCompleted.value) {
                submitTest()
            }
        }
    }

    fun selectOption(questionIndex: Int, option: Int) {
        val current = _userAnswers.value.toMutableMap()
        current[questionIndex] = option
        _userAnswers.value = current
    }

    fun toggleMarkForReview(questionIndex: Int) {
        val current = _markedForReview.value.toMutableSet()
        if (current.contains(questionIndex)) {
            current.remove(questionIndex)
        } else {
            current.add(questionIndex)
        }
        _markedForReview.value = current
    }

    fun navigateToQuestion(index: Int) {
        if (index in 0 until _currentQuestions.value.size) {
            _currentQuestionIndex.value = index
        }
    }

    fun nextQuestion() {
        if (_currentQuestionIndex.value < _currentQuestions.value.size - 1) {
            _currentQuestionIndex.value += 1
        }
    }

    fun previousQuestion() {
        if (_currentQuestionIndex.value > 0) {
            _currentQuestionIndex.value -= 1
        }
    }

    fun submitTest() {
        timerJob?.cancel()
        val questions = _currentQuestions.value
        val answers = _userAnswers.value
        val totalQ = questions.size
        var correct = 0
        var wrong = 0
        var unattempted = 0

        var mathCorrect = 0
        var mathTotal = 0
        var reasoningCorrect = 0
        var reasoningTotal = 0
        var gkCorrect = 0
        var gkTotal = 0
        var hindiEngCorrect = 0
        var hindiEngTotal = 0

        questions.forEachIndexed { index, q ->
            val userAns = answers[index]
            val isMath = q.subjectName == "Mathematics"
            val isReasoning = q.subjectName == "Reasoning"
            val isGk = q.subjectName.contains("GK")
            val isLang = q.subjectName == "Hindi" || q.subjectName == "English"

            if (isMath) mathTotal++
            if (isReasoning) reasoningTotal++
            if (isGk) gkTotal++
            if (isLang) hindiEngTotal++

            if (userAns == null) {
                unattempted++
            } else if (userAns == q.correctOption) {
                correct++
                if (isMath) mathCorrect++
                if (isReasoning) reasoningCorrect++
                if (isGk) gkCorrect++
                if (isLang) hindiEngCorrect++
            } else {
                wrong++
            }
        }

        val marksPerQ = 2.0
        val negPerQ = 0.5
        val rawScore = (correct * marksPerQ) - (wrong * negPerQ)
        val maxScore = totalQ * marksPerQ
        val finalScore = rawScore.coerceAtLeast(0.0)
        val attempted = correct + wrong
        val accuracy = if (attempted > 0) (correct.toDouble() / attempted * 100.0) else 0.0
        val timeTaken = testTotalDurationSeconds - _timeRemainingSeconds.value

        val weakSubject = when {
            gkTotal > 0 && (gkCorrect.toDouble() / gkTotal) < 0.6 -> "GK / GS & Current Affairs"
            mathTotal > 0 && (mathCorrect.toDouble() / mathTotal) < 0.6 -> "Mathematics (गणित)"
            reasoningTotal > 0 && (reasoningCorrect.toDouble() / reasoningTotal) < 0.6 -> "Reasoning (तर्कशक्ति)"
            else -> "सामान्य अध्ययन व रिवीज़न"
        }

        val strongSubject = when {
            mathTotal > 0 && (mathCorrect.toDouble() / mathTotal) >= 0.7 -> "Mathematics (गणित)"
            reasoningTotal > 0 && (reasoningCorrect.toDouble() / reasoningTotal) >= 0.7 -> "Reasoning (तर्कशक्ति)"
            gkTotal > 0 && (gkCorrect.toDouble() / gkTotal) >= 0.7 -> "GK / GS"
            else -> "हिंदी व्याकरण व सामान्य ज्ञान"
        }

        val attempt = TestAttempt(
            testId = activeTestId,
            testTitle = _testTitle.value,
            targetExam = _targetExam.value,
            studentId = _activeStudentId.value,
            date = todayDateStr,
            totalQuestions = totalQ,
            attemptedCount = attempted,
            correctCount = correct,
            wrongCount = wrong,
            unattemptedCount = unattempted,
            score = finalScore,
            maxScore = maxScore,
            accuracyPercentage = Math.round(accuracy * 10.0) / 10.0,
            timeTakenSeconds = timeTaken,
            rank = if (finalScore >= maxScore * 0.8) 1 else 2,
            percentile = if (finalScore >= maxScore * 0.8) 95.5 else 82.0,
            mathScore = "$mathCorrect/${mathTotal.coerceAtLeast(1)}",
            reasoningScore = "$reasoningCorrect/${reasoningTotal.coerceAtLeast(1)}",
            gkScore = "$gkCorrect/${gkTotal.coerceAtLeast(1)}",
            hindiEnglishScore = "$hindiEngCorrect/${hindiEngTotal.coerceAtLeast(1)}",
            weakArea = weakSubject,
            strongArea = strongSubject
        )

        _latestAttemptResult.value = attempt
        _isTestCompleted.value = true

        viewModelScope.launch {
            repository.recordTestAttempt(attempt)
        }
    }

    // --- Admin Actions ---
    fun addStudent(student: StudentProfile, onResult: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            val trimmedMobile = student.mobileNumber.trim()
            val digitsOnly = trimmedMobile.filter { it.isDigit() }
            if (digitsOnly.length != 10) {
                onResult(Result.failure(IllegalArgumentException("कृपया 10 अंकों का वैध मोबाइल नंबर दर्ज करें!")))
                return@launch
            }
            val exists = repository.isMobileRegistered(trimmedMobile)
            if (exists) {
                onResult(Result.failure(IllegalArgumentException("मोबाइल नंबर ($trimmedMobile) पहले से पंजीकृत है!")))
                return@launch
            }
            repository.insertStudent(student.copy(mobileNumber = trimmedMobile))
            onResult(Result.success(Unit))
        }
    }

    fun updateStudent(student: StudentProfile) {
        viewModelScope.launch {
            repository.updateStudent(student)
        }
    }

    fun deleteStudent(student: StudentProfile) {
        viewModelScope.launch {
            repository.deleteStudent(student)
        }
    }

    fun selectRecruitmentCategory(catId: String) {
        _selectedRecruitmentCategory.value = catId
    }

    fun selectNoticeCategory(category: String) {
        _selectedNoticeCategory.value = category
    }

    fun setNoticeSearchQuery(query: String) {
        _noticeSearchQuery.value = query
    }

    fun publishNotice(title: String, content: String, category: String, isUrgent: Boolean) {
        viewModelScope.launch {
            val notice = Notice(
                title = title,
                content = content,
                category = category,
                date = todayDateStr,
                author = "जय बजरंग अखाड़ा प्रशासक",
                isUrgent = isUrgent,
                priority = if (isUrgent) "URGENT" else "NORMAL"
            )
            repository.addNotice(notice)
        }
    }

    fun publishNoticeWithDetails(
        title: String,
        content: String,
        category: String,
        priority: String = "NORMAL",
        isPinned: Boolean = false,
        expiryDate: String = "",
        author: String = "मुख्य प्रशिक्षक (Head Trainer)"
    ) {
        viewModelScope.launch {
            val notice = Notice(
                title = title,
                content = content,
                category = category,
                date = todayDateStr,
                author = author,
                isUrgent = priority.equals("URGENT", true) || priority.equals("HIGH", true),
                priority = priority,
                isPinned = isPinned,
                expiryDate = expiryDate
            )
            repository.addNotice(notice)
        }
    }

    fun updateNotice(notice: Notice) {
        viewModelScope.launch {
            repository.updateNotice(notice)
        }
    }

    fun togglePinNotice(notice: Notice) {
        viewModelScope.launch {
            repository.togglePinNotice(notice)
        }
    }

    fun toggleNoticePin(notice: Notice) {
        togglePinNotice(notice)
    }

    fun markNoticeRead(notice: Notice) {
        viewModelScope.launch {
            repository.markNoticeRead(notice)
        }
    }

    fun deleteNotice(notice: Notice) {
        viewModelScope.launch {
            repository.deleteNotice(notice)
        }
    }

    fun createWorkoutPlan(plan: DailyWorkoutPlan) {
        viewModelScope.launch {
            repository.setWorkoutPlan(plan)
        }
    }

    fun addRecruitmentInfo(info: RecruitmentInfo) {
        viewModelScope.launch {
            repository.addRecruitmentInfo(info)
        }
    }

    fun updateRecruitmentInfo(info: RecruitmentInfo) {
        viewModelScope.launch {
            repository.updateRecruitmentInfo(info)
        }
    }

    fun deleteRecruitmentInfo(info: RecruitmentInfo) {
        viewModelScope.launch {
            repository.deleteRecruitmentInfo(info)
        }
    }

    fun addQuestion(question: Question) {
        viewModelScope.launch {
            repository.addQuestion(question)
        }
    }

    fun updateQuestion(question: Question) {
        viewModelScope.launch {
            repository.updateQuestion(question)
        }
    }

    fun deleteQuestion(question: Question) {
        viewModelScope.launch {
            repository.deleteQuestion(question)
        }
    }

    fun toggleQuestionActive(question: Question) {
        viewModelScope.launch {
            repository.updateQuestion(question.copy(isActive = !question.isActive))
        }
    }

    fun addSubject(subject: StudySubject) {
        viewModelScope.launch {
            repository.insertSubject(subject)
        }
    }

    fun updateSubject(subject: StudySubject) {
        viewModelScope.launch {
            repository.updateSubject(subject)
        }
    }

    fun deleteSubject(subject: StudySubject) {
        viewModelScope.launch {
            repository.deleteSubject(subject)
        }
    }

    fun addTopic(topic: StudyTopic) {
        viewModelScope.launch {
            repository.insertTopic(topic)
        }
    }

    fun updateTopic(topic: StudyTopic) {
        viewModelScope.launch {
            repository.updateTopic(topic)
        }
    }

    fun deleteTopic(topic: StudyTopic) {
        viewModelScope.launch {
            repository.deleteTopic(topic)
        }
    }

    fun recordStudyQuestionAttempt(
        questionId: String,
        subjectId: String,
        topicId: String,
        selectedAnswer: String,
        isCorrect: Boolean,
        timeTakenSeconds: Int = 0,
        studentIdOverride: String? = null
    ) {
        viewModelScope.launch {
            val studentId = studentIdOverride ?: _activeStudentId.value
            val attempt = StudyAttempt(
                studentId = studentId,
                questionId = questionId,
                subjectId = subjectId,
                topicId = topicId,
                selectedAnswer = selectedAnswer,
                isCorrect = isCorrect,
                timeTakenSeconds = timeTakenSeconds,
                attemptDate = todayDateStr
            )
            repository.recordStudyAttempt(attempt)
        }
    }

    fun addMockTest(mockTest: MockTest) {
        viewModelScope.launch {
            repository.addMockTest(mockTest)
        }
    }

    fun markAdminAttendanceBatch(studentStatusMap: Map<String, String>, date: String = todayDateStr) {
        viewModelScope.launch {
            val records = studentStatusMap.map { (sId, status) ->
                AttendanceRecord(
                    studentId = sId,
                    date = date,
                    status = status,
                    remarks = "प्रशिक्षक द्वारा सत्यापित"
                )
            }
            repository.markBatchAttendance(records)
        }
    }

    // ==========================================
    // Phase 3A: Firebase Cloud Sync & Auth Flows
    // ==========================================

    val cloudSyncStatus: StateFlow<com.example.data.cloud.CloudSyncStatus> = repository.cloudSyncStatus
    val cloudAuthState: StateFlow<com.example.data.cloud.CloudAuthState> = repository.cloudAuthState

    fun triggerCloudSync() {
        viewModelScope.launch {
            repository.syncWithCloud()
        }
    }

    fun signInWithCloudEmail(email: String, pinOrPass: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val result = repository.cloudAuthManager.signInWithEmail(email, pinOrPass)
            result.onSuccess { auth ->
                _currentRole.value = auth.role
                if (!auth.studentId.isNullOrBlank()) {
                    _activeStudentId.value = auth.studentId
                }
                _isLoggedIn.value = true
                onResult(true, null)
            }.onFailure { error ->
                onResult(false, error.message)
            }
        }
    }

    fun registerCloudUser(
        email: String,
        pass: String,
        name: String,
        role: String = com.example.data.cloud.FirestoreConstants.ROLE_STUDENT,
        studentId: String = "",
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val result = repository.cloudAuthManager.registerUser(email, pass, name, role, studentId)
            result.onSuccess { auth ->
                _currentRole.value = auth.role
                if (studentId.isNotBlank()) {
                    _activeStudentId.value = studentId
                }
                _isLoggedIn.value = true
                onResult(true, null)
            }.onFailure { error ->
                onResult(false, error.message)
            }
        }
    }

    fun signOutCloud() {
        repository.cloudAuthManager.signOut()
        _isLoggedIn.value = false
    }
}
