// src/App.jsx
import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';

import RequireRegistration from './components/RequireRegistration';
import RequireLogin        from './components/RequireLogin';

import RegisterPage        from './pages/RegisterPage';
import LoginPage           from './pages/LoginPage';
import SideMenu            from './components/SideMenu';
import SellerLayout        from './components/SellerLayout';
import SellerPage          from './pages/SellerPage';
import MyListingsPage      from './pages/MyListingsPage';
import AdminPage           from './pages/AdminPage';
import { SectionProvider } from './context/SectionContext';
import { ProductsProvider } from './context/ProductsContext'; // Add this import

function App() {
  return (
      <Router>
        <ProductsProvider> {/* Wrap all routes with ProductsProvider */}
          <Routes>
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route element={<RequireLogin />}>
              <Route path="/admin" element={<AdminPage />} />
              <Route
                  path="/explore"
                  element={
                    <SectionProvider>
                      <SideMenu />
                    </SectionProvider>
                  }
              />
              <Route path="/seller" element={<SellerLayout />}>
                <Route index element={<SellerPage />} />
                <Route path="listings" element={<MyListingsPage />} />
              </Route>
            </Route>
            <Route
                path="/"
                element={
                  localStorage.getItem('isRegistered') !== 'true'
                      ? <Navigate to="/register" replace />
                      : localStorage.getItem('isLoggedIn') !== 'true'
                          ? <Navigate to="/login" replace />
                          : localStorage.getItem('adminLoggedIn') === 'true'
                              ? <Navigate to="/admin" replace />
                              : localStorage.getItem('userRole') === 'seller'
                                  ? <Navigate to="/seller" replace />
                                  : <Navigate to="/explore" replace />
                }
            />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </ProductsProvider>
      </Router>
  );
}

export default App;