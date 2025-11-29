import { Navigate, Outlet } from 'react-router-dom';

export default function RequireRegistration() {
  const isRegistered = localStorage.getItem('isRegistered') === 'true';
  return isRegistered
    ? <Outlet />
    : <Navigate to="/register" replace />;
}
