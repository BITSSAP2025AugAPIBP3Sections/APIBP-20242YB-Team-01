// src/pages/MyAuctionPage.jsx
import React, { useState, useEffect } from 'react';
import styles from '../css/MyAuctionPage.module.css';
import { jwtDecode } from 'jwt-decode';
import appliances from '../assets/categories/appliances.jpg';
import automotive from '../assets/categories/automotive.jpg';
import beauty from '../assets/categories/beauty.jpg';
import books from '../assets/categories/books.jpg';
import electronics from '../assets/categories/electronics.jpg';
import fashion from '../assets/categories/fashion.jpg';
import garden from '../assets/categories/garden.jpg';
import music from '../assets/categories/music.jpg';
import sports from '../assets/categories/sports.jpg';
import toys from '../assets/categories/toys.jpg';
import defaultImg from '../assets/categories/all.jpg';

const TABS = [
  { key: 'inProgress', label: 'In-Progress Bids' },
  { key: 'won',        label: 'Won Bids'         },
  { key: 'closed',     label: 'Closed Bids'      },
  { key: 'all',        label: 'All'              }
];

export default function MyAuctionsPage() {
  const [activeTab, setActiveTab] = useState('inProgress');
  const [bids, setBids] = useState([]);
  const [loading, setLoading] = useState(true);
  const [userId, setUserId] = useState(null);

  // Decode JWT to get userId on mount
  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      const decoded = jwtDecode(token);
      setUserId(decoded.userId || decoded.id || decoded.sub);
    }
  }, []);

  const categoryImageMap = {
    HOME_APPLIANCES: appliances,
    AUTOMOTIVE: automotive,
    BEAUTY: beauty,
    BOOKS: books,
    ELECTRONICS: electronics,
    FASHION: fashion,
    GARDEN: garden,
    MUSIC: music,
    SPORTS: sports,
    TOYS: toys,
    DEFAULT: defaultImg,
  };

  // Fetch bids once userId is known
  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token || !userId) return;
    setLoading(true);

    // Use the bidding summary endpoint that provides comprehensive bid data
    fetch(`http://localhost:8080/api/bids/v1/users/${userId}/summary`, {
      headers: { 'Authorization': `Bearer ${token}` }
    })
        .then(res => {
          if (!res.ok) throw new Error('Network error');
          return res.json();
        })
        .then(async data => {
          // Enrich each bid with a productImage from category data
          const enrichedBids = await Promise.all(
              data.map(async bid => {
                try {
                  // Fetch full product details to get the category
                  const res = await fetch(
                      `http://localhost8080/api/products/v1/${bid.productId}`,
                      { headers: { 'Authorization': `Bearer ${token}` } }
                  );
                  const product = await res.json();
                  const category = product.category?.toUpperCase() || "DEFAULT";
                  const productImage = categoryImageMap[category] || categoryImageMap["DEFAULT"];
                  // Combine bid summary with product details
                  return { 
                    ...bid, 
                    productImage,
                    frozen: product.frozen,
                    sold: product.sold
                  };
                } catch (err) {
                  return { ...bid, productImage: categoryImageMap["DEFAULT"] };
                }
              })
          );
          setBids(enrichedBids);
          setLoading(false);
        })
        .catch(() => setLoading(false));
  }, [userId]);

  const now = new Date();

  // Classify based on bid status from BiddingSummaryDTO
  // Status can be: "Winning", "Outbid", "Lost", "Won"
  const classifyBids = (bids) => {
    if (!userId) {
      return { inProgress: [], won: [], closed: [], all: [] };
    }
    return {
      // In-progress: Active bids (Winning or Outbid) where auction hasn't ended
      inProgress: bids.filter(bid => {
        const isActive = bid.status === 'Winning' || bid.status === 'Outbid';
        const notEnded = bid.endTime && new Date(bid.endTime) > now;
        return isActive && notEnded;
      }),
      // Won: Bids with "Won" status or sold products where this user is winner
      won: bids.filter(bid => bid.status === 'Won' || bid.sold),
      // Closed: Bids that are "Lost" or auction ended without winning
      closed: bids.filter(bid => {
        const isLost = bid.status === 'Lost';
        const isFrozen = bid.frozen && bid.status !== 'Won';
        const hasEnded = bid.endTime && new Date(bid.endTime) <= now && bid.status !== 'Won';
        return isLost || isFrozen || hasEnded;
      }),
      all: bids
    };
  };

  const classified = classifyBids(bids);
  const cards = classified[activeTab] || [];

  return (
      <div className={styles.root}>
        <h1 className={styles.heading}>My Auctions</h1>

        <nav className={styles.tabBar}>
          {TABS.map(tab => (
              <button
                  key={tab.key}
                  className={`${styles.tabButton} ${tab.key === activeTab ? styles.active : ''}`}
                  onClick={() => setActiveTab(tab.key)}
              >
                {tab.label}
              </button>
          ))}
        </nav>

        <div className={styles.cardsGrid}>
          {loading ? (
              <p>Loading...</p>
          ) : cards.length === 0 ? (
              <p className={styles.emptyMessage}>No items in this category.</p>
          ) : (
              cards.map(bid => (
                  <div key={bid.id} className={styles.card}>
                    <div className={styles.imageWrapper}>
                      <img
                          src={bid.productImage || '/images/default.png'}
                          alt={bid.productName || 'Product'}
                          className={styles.cardImage}
                      />
                      {bid.status === 'Winning' && (
                          <span className={styles.badgeLive}>Winning</span>
                      )}
                      {bid.status === 'Outbid' && (
                          <span className={styles.badgeOutbid}>Outbid</span>
                      )}
                      {bid.status === 'Won' && (
                          <span className={styles.badgeWon}>Won</span>
                      )}
                      {bid.status === 'Lost' && (
                          <span className={styles.badgeClosed}>Lost</span>
                      )}
                    </div>
                    <h3 className={styles.cardTitle}>{bid.productName || 'Unnamed Product'}</h3>
                    <div className={styles.bidInfo}>
                      Your Bid: <strong>${bid.amount?.toLocaleString()}</strong>
                    </div>
                    <div className={styles.bidInfo}>
                      Status: <strong>{bid.status}</strong>
                    </div>
                    {bid.endTime && (
                        <div className={styles.bidInfo}>
                          {new Date(bid.endTime) > now 
                            ? `Ends: ${new Date(bid.endTime).toLocaleString()}`
                            : `Ended: ${new Date(bid.endTime).toLocaleString()}`
                          }
                        </div>
                    )}
                  </div>
              ))
          )}
        </div>
      </div>
  );
}
