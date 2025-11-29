import React, { useState } from 'react';
import favData from '../data/favourites.json';    // your JSON of favourite items
import styles from '../css/FavouritesPage.module.css';
import walletData from '../data/UserProfile.json';

export default function FavouritesPage() {
  const [searchText, setSearchText] = useState('');
  const favourites = favData.items;  

  return (
    <div className={styles.root}>
      {/* Top Bar (same as other pages) */}
      <div className={styles.topBar}>
        <div className={styles.search}>
          <input
            type="text"
            value={searchText}
            onChange={e => setSearchText(e.target.value)}
            placeholder="Search Favourites"
            className={styles.searchInput}
          />
        </div>
        <div className={styles.wallet}>
          ${walletData.wallet}
        </div>
      </div>

      {/* Favourites Grid */}
      <div className={styles.section}>
        <h2 className={styles.sectionTitle}>Your Favourites</h2>
        <div className={styles.cards}>
          {favourites
            .filter(item =>
              item.title.toLowerCase().includes(searchText.toLowerCase())
            )
            .map(item => (
              <div key={item.id} className={styles.card}>
                <div
                  className={styles.cardImage}
                  style={{ backgroundImage: `url(${item.image})` }}
                />
                <h3 className={styles.cardTitle}>{item.title}</h3>
                <p className={styles.cardPrice}>
                  ${item.price.toLocaleString()}
                </p>
                <button className={styles.removeButton}>
                  Remove
                </button>
              </div>
            ))}
        </div>
      </div>
    </div>
  );
}
