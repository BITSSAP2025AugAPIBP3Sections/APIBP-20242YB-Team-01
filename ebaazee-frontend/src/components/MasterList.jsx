import React from 'react';
import { useNavigate } from 'react-router-dom';
import logoSrc from '../assets/Logo.png';
import styles from '../css/MasterList.module.css';

export default function MasterList({ selectedSection, onSelectSection }) {
    const marketplaceItems = [
        { key: 'explore', label: 'Explore' }
    ];

    // Removed the { key: 'payment', label: 'Payment' } line below
    const userItems = [
        { key: 'dashboard',    label: 'Dashboard' },
        { key: 'myauction',    label: 'My Auction' },
        { key: 'helpandsupport', label: 'Help and Support' },
    ];

    const navigate = useNavigate();
    const handleLogout = () => {
        localStorage.removeItem('isLoggedIn');
        localStorage.removeItem('adminLoggedIn');
        navigate('/login', { replace: true });
    };

    return (
        <aside className={styles.sidebar}>
            {/* Logo */}
            <div className={styles.logoContainer}>
                <img src={logoSrc} alt="App Logo" className={styles.logo} />
            </div>

            {/* Marketplace Section */}
            <nav className={styles.section}>
                <div className={styles.sectionHeading}>Marketplace</div>
                <ul className={styles.itemList}>
                    {marketplaceItems.map(item => (
                        <li
                            key={item.key}
                            onClick={() => onSelectSection(item.key)}
                            className={`${styles.item} ${selectedSection === item.key ? styles.selected : ''}`}
                        >
                            {item.label}
                        </li>
                    ))}
                </ul>
            </nav>

            {/* User Section */}
            <nav className={styles.section}>
                <div className={styles.sectionHeading}>User</div>
                <ul className={styles.itemList}>
                    {userItems.map(item => (
                        <li
                            key={item.key}
                            onClick={() => onSelectSection(item.key)}
                            className={`${styles.item} ${selectedSection === item.key ? styles.selected : ''}`}
                        >
                            {item.label}
                        </li>
                    ))}
                </ul>
            </nav>

            {/* Log Out Section */}
            <nav className={styles.section}>
                <ul className={styles.itemList}>
                    <button
                        className={styles.logoutBtn}
                        onClick={handleLogout}
                    >
                        Logout
                    </button>
                </ul>
            </nav>
        </aside>
    );
}
 