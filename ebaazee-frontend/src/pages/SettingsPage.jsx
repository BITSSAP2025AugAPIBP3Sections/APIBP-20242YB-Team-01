import React, { useState, useEffect } from 'react';
import userData from '../data/UserProfile.json';
import styles from '../css/SettingsPage.module.css';
import defaultAvatars from '../data/defaultAvatars.json';

export default function SettingsPage() {
  const [avatar, setAvatar] = useState(userData.avatarUrl?.trim() || null);
  const [firstName, setFirstName] = useState(userData.firstName);
  const [lastName, setLastName] = useState(userData.lastName);
  const [email, setEmail] = useState(userData.email);
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) return;
    fetch('http://localhost:9090/api/user/profile', {
      headers: { Authorization: `Bearer ${token}` }
    })
        .then(res => res.json())
        .then(data => {
          setFirstName(data.firstName);
          setLastName(data.lastName);
          setEmail(data.username);
        })
        .catch(() => console.log('error'));
  }, []);

  useEffect(() => {
    if (!avatar) {
      const randomIndex = Math.floor(Math.random() * defaultAvatars.length);
      setAvatar(defaultAvatars[randomIndex]);
    }
  }, [avatar]);

  const handleSave = () => {
    setError('');
    setSuccessMessage('');

    if (newPassword || confirmPassword) {
      if (newPassword !== confirmPassword) {
        setError('Passwords do not match');
        return;
      }
      if (newPassword.length < 6) {
        setError('Password must be at least 6 characters long');
        return;
      }
    }

    const token = localStorage.getItem('token');
    if (!token) {
      alert('Not authenticated');
      return;
    }

    fetch('http://localhost:9090/api/user/profile', {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`
      },
      body: JSON.stringify({
        firstName,
        lastName,
        password: newPassword || undefined
      })
    })
        .then(res => {
          if (!res.ok) throw new Error('Update failed');
          if (newPassword) {
            alert('Password changed. Please login again.');
            localStorage.removeItem('token');
            window.location.href = '/login';
          } else {
            setSuccessMessage('Profile updated successfully');
            setNewPassword('');
            setConfirmPassword('');
          }
        })
        .catch(() => setError('Something went wrong. Please try again.'));
  };

  return (
      <div className={styles.root}>
        <section className={styles.card}>
          <div className={styles.avatarWrapper}>
            {avatar && <img src={avatar} alt="Avatar" className={styles.avatar} />}
          </div>

          <h2 className={styles.heading}>Update Your Profile</h2>
          <hr className={styles.divider} />

          <div className={styles.formGrid}>
            <div className={styles.formGroup}>
              <label htmlFor="firstName">First Name</label>
              <input
                  id="firstName"
                  type="text"
                  value={firstName}
                  onChange={e => setFirstName(e.target.value)}
              />
            </div>

            <div className={styles.formGroup}>
              <label htmlFor="lastName">Last Name</label>
              <input
                  id="lastName"
                  type="text"
                  value={lastName}
                  onChange={e => setLastName(e.target.value)}
              />
            </div>

            <div className={`${styles.fullWidth} ${styles.formGroup}`}>
              <label htmlFor="email">Email Address</label>
              <input id="email" type="email" value={email} disabled />
            </div>

            <div className={`${styles.fullWidth} ${styles.formGroup}`}>
              <label htmlFor="newPassword">New Password</label>
              <input
                  id="newPassword"
                  type="password"
                  value={newPassword}
                  onChange={e => setNewPassword(e.target.value)}
                  placeholder="Enter new password"
              />
            </div>

            <div className={`${styles.fullWidth} ${styles.formGroup}`}>
              <label htmlFor="confirmPassword">Confirm Password</label>
              <input
                  id="confirmPassword"
                  type="password"
                  value={confirmPassword}
                  onChange={e => setConfirmPassword(e.target.value)}
                  placeholder="Re-enter new password"
              />
            </div>
          </div>

          {error && <div className={styles.error}>{error}</div>}
          {successMessage && <div className={styles.success}>{successMessage}</div>}

          <button className={styles.saveButton} onClick={handleSave}>
            Save Changes
          </button>
        </section>
      </div>
  );
}
