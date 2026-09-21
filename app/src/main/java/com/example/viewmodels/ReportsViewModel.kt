package com.example.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.models.UserReport
import com.example.repository.AdminRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ReportsViewModel : ViewModel() {

    private val repository = AdminRepository()
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _selectedFilter = MutableStateFlow("ALL")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    val reports: StateFlow<List<UserReport>> = repository.getReports()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val filteredReports: StateFlow<List<UserReport>> = combine(reports, _selectedFilter) { list, filter ->
        if (filter == "ALL") list else list.filter { it.status.equals(filter, ignoreCase = true) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun resolveReport(reportId: String, status: String, notes: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                repository.updateReportStatus(reportId, status, notes)
                _statusMessage.value = "Report marked as $status"
            } catch (e: Exception) {
                _statusMessage.value = "Error updating report: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun deleteReport(reportId: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                repository.deleteReport(reportId)
                _statusMessage.value = "Report deleted"
            } catch (e: Exception) {
                _statusMessage.value = "Error deleting report: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }
}
