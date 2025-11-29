// src/components/seller/SellerMasterList.jsx
import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import styles from '../css/SellerMasterList.module.css';
import logoSrc from '../assets/Logo.png';

export default function SellerMasterList() {
  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.removeItem('isLoggedIn');
    localStorage.removeItem('adminLoggedIn');
    navigate('/login', { replace: true });
  };

  return (
    <aside className={styles.sidebar}>
      <div className={styles.logoContainer}>
        <img src={logoSrc} alt="App Logo" className={styles.logo} />
      </div>
      <nav className={styles.nav}>
        <NavLink
          to="/seller"
          end
          className={({ isActive }) =>
            isActive ? styles.activeLink : styles.link
          }
        >
          Create Listing
        </NavLink>
        <NavLink
          to="/seller/listings"
          className={({ isActive }) =>
            isActive ? styles.activeLink : styles.link
          }
        >
          My Listings
        </NavLink>
      </nav>
      <button className={styles.logout} onClick={handleLogout}>
        Logout
      </button>
    </aside>
  );
}
