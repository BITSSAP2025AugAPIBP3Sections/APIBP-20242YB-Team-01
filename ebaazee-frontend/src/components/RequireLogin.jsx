import { Navigate, Outlet } from 'react-router-dom';

export default function RequireLogin() {
  const isLoggedIn = localStorage.getItem('isLoggedIn') === 'true';
  return isLoggedIn
    ? <Outlet />
    : <Navigate to="/login" replace />;
}
