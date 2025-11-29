import React, { useRef, useState, useEffect } from 'react';
import rawData from '../data/ActiveBidsData.json';
import styles from '../css/ActiveBids.module.css';
import walletData from '../data/UserProfile.json';

export default function ActiveBidPage() {
  const {
    liveAuctions,
    popularAuctions,
    highestBids,
    upcomingAuctions
  } = rawData;
  const [searchText, setSearchText] = useState('');

  return (
    <div className={styles.root}>
          {/* 1. Top Bar */}
          <div className={styles.topBar}>
            <div className={styles.search}>
              <input
                type="text"
                value={searchText}
                onChange={e => setSearchText(e.target.value)}
                placeholder="Search"
                className={styles.searchInput}
              />
            </div>
            <div className={styles.wallet}>
              ${walletData.wallet}
            </div>
          </div>

      {/* Four carousel sections */}
      <CarouselSection title="Live Auctions" items={liveAuctions} />
      <CarouselSection title="Popular Auctions" items={popularAuctions} />
      <CarouselSection title="Highest Bidding Auctions" items={highestBids} />
      <CarouselSection title="Upcoming Auctions" items={upcomingAuctions} />
    </div>
  );
}

function CarouselSection({ title, items }) {
  const scrollRef = useRef(null);
  const [current, setCurrent] = useState(0);

  useEffect(() => {
    const container = scrollRef.current;
    if (container) {
             // calculate the new scroll position so the card is centered
             const card = container.children[current];
             const cardCenter = card.offsetLeft + card.offsetWidth / 2;
             const containerCenter = container.clientWidth / 2;
             container.scrollTo({
               left: cardCenter - containerCenter,
               behavior: 'smooth'
             });
    }
  }, [current]);

  const prev = () => setCurrent(i => Math.max(0, i - 1));
  const next = () => setCurrent(i => Math.min(items.length - 1, i + 1));

  return (
    <div className={styles.section}>
      <h2 className={styles.sectionTitle}>{title}</h2>
      <div className={styles.carouselWrapper}>
        <button
          onClick={prev}
          disabled={current === 0}
          className={`${styles.carouselArrow} ${styles.left}`}
        > 
          ‹ 
        </button>

        <div className={styles.carousel} ref={scrollRef}>
          {items.map((item) => (
            <div key={item.id} className={styles.card}>
              {/* Image */}
              <div
                className={styles.cardImage}
                style={{ backgroundImage: `url(${item.image})` }}
              />
              {/* Countdown */}
              <div className={styles.countdown}>
                {['Days','Hours','Min','Sec'].map(lbl => (
                  <div key={lbl} className={styles.timeSegment}>
                    <span>00</span><small>{lbl}</small>
                  </div>
                ))}
              </div>
              {/* Title & Bid */}
              <h3 className={styles.cardTitle}>{item.title}</h3>
              <div className={styles.currentBid}>
                Current Bid at: <strong>${item.price.toLocaleString()}</strong>
              </div>
              <button className={styles.bidButton}>
                {item.status === 'Active' ? 'Bid Now' : 'Notify Me'}
              </button>
            </div>
          ))}
        </div>

        <button
          onClick={next}
          disabled={current === items.length - 1}
          className={`${styles.carouselArrow} ${styles.right}`}
        >
          ›
        </button>
      </div>
    </div>
  );
}
