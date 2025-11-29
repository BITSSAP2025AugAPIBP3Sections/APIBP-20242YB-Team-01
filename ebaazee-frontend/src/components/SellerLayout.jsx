// src/components/seller/SellerLayout.jsx
import React from 'react';
import { Outlet } from 'react-router-dom';
import SellerMasterList from './SellerMasterList';
import styles from '../css/SellerLayout.module.css';

export default function SellerLayout() {
  return (
    <div className={styles.container}>
      <SellerMasterList />
      <main className={styles.content}>
        <Outlet />
      </main>
    </div>
  );
}
