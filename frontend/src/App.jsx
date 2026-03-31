import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { LandingPage } from './pages/LandingPage';
import { AuthPage } from './pages/AuthPage';
import { Dashboard } from './pages/Dashboard';
import { DeploymentDetail } from './pages/DeploymentDetail';
import { NewDeployment } from './pages/NewDeployment';
import { ProtectedRoute } from './components/ProtectedRoute';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/login" element={<AuthPage />} />
        <Route path="/signup" element={<AuthPage />} />
        <Route path="/dashboard" element={<ProtectedRoute><Dashboard /></ProtectedRoute>} />
        <Route path="/deployments/:id" element={<ProtectedRoute><DeploymentDetail /></ProtectedRoute>} />
        <Route path="/deploy/new" element={<ProtectedRoute><NewDeployment /></ProtectedRoute>} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
