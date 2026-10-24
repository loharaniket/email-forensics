import React from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import ProtectedRoute from './components/ProtectedRoute'
import LoginPage from './pages/LoginPage'
import CasesPage from './pages/CasesPage'
import CaseDetailPage from './pages/CaseDetailPage'
import HealthPage from './pages/HealthPage'

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            path="/cases"
            element={
              <ProtectedRoute>
                <CasesPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/cases/:caseId"
            element={
              <ProtectedRoute>
                <CaseDetailPage />
              </ProtectedRoute>
            }
          />
          <Route path="/health" element={<HealthPage />} />
          <Route path="/" element={<Navigate to="/cases" replace />} />
          <Route path="*" element={<Navigate to="/cases" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
