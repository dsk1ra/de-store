// Shared utility functions for DE-Store web interface

const BASE_URL = 'http://localhost:8080';

// Check authentication
function checkAuth() {
    const token = sessionStorage.getItem('accessToken');
    if (!token) {
        window.location.href = 'login.html';
        return false;
    }
    return true;
}

// Get authorization headers
function getHeaders() {
    const token = sessionStorage.getItem('accessToken');
    return {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
    };
}

// Show loading state on button
function showLoading(button) {
    button.disabled = true;
    const originalText = button.textContent;
    button.setAttribute('data-original-text', originalText);
    button.innerHTML = originalText + ' <span class="loading"></span>';
}

// Hide loading state on button
function hideLoading(button) {
    button.disabled = false;
    const originalText = button.getAttribute('data-original-text');
    if (originalText) {
        button.innerHTML = originalText;
    }
}

// Show response in a designated area
function showResponse(elementId, data, isError = false) {
    const element = document.getElementById(elementId);
    if (!element) return;

    element.style.display = 'block';
    element.className = 'response-area ' + (isError ? 'error' : 'success');
    element.textContent = JSON.stringify(data, null, 2);
}

// Show error message
function showError(message) {
    alert(message);
}

// Show success message
function showSuccess(message) {
    alert(message);
}

// Handle API errors
function handleApiError(error, context = '') {
    console.error(`API Error ${context}:`, error);
    const message = error.message || 'An error occurred';
    showError(`Error ${context}: ${message}`);
}

// Navigate back to dashboard
function goToDashboard() {
    window.location.href = 'dashboard.html';
}

// Logout function
function handleLogout() {
    if (confirm('Are you sure you want to logout?')) {
        sessionStorage.clear();
        window.location.href = '../login.html';
    }
}

// Initialize page header with user info
function initializePageHeader() {
    const username = sessionStorage.getItem('username');
    const role = sessionStorage.getItem('role');
    const userNameElement = document.getElementById('userName');

    if (userNameElement && username) {
        userNameElement.textContent = `${username} (${role || 'User'})`;
    }
}

// Format date for display
function formatDate(dateString) {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleString();
}

// Toggle between JSON and table view
function toggleView(elementId, viewType) {
    const element = document.getElementById(elementId);
    const jsonBtn = document.getElementById(elementId + '-json-btn');
    const tableBtn = document.getElementById(elementId + '-table-btn');

    if (viewType === 'json') {
        jsonBtn?.classList.add('active');
        tableBtn?.classList.remove('active');
    } else {
        jsonBtn?.classList.remove('active');
        tableBtn?.classList.add('active');
    }
}
